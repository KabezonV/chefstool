package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.Ficha;
import com.chefstool.backend.domain.model.FichaLinea;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.ProveedorProducto;
import com.chefstool.backend.domain.port.out.FichaLineaRepository;
import com.chefstool.backend.domain.port.out.FichaRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorProductoRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import com.chefstool.backend.adapter.in.web.dto.FichaLineaRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaLineaResponse;
import com.chefstool.backend.adapter.in.web.dto.FichaRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaResumen;
import com.chefstool.backend.application.dto.Pagina;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fichas tecnicas (escandallos). El coste de cada linea se calcula con el mejor
 * precio vigente del producto entre todos los proveedores.
 */
@Service
public class FichaService {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final FichaRepository fichaRepository;
    private final FichaLineaRepository fichaLineaRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorProductoRepository proveedorProductoRepository;
    private final OrganizacionService organizacionService;

    public FichaService(
            FichaRepository fichaRepository,
            FichaLineaRepository fichaLineaRepository,
            ProductoRepository productoRepository,
            ProveedorProductoRepository proveedorProductoRepository,
            OrganizacionService organizacionService) {
        this.fichaRepository = fichaRepository;
        this.fichaLineaRepository = fichaLineaRepository;
        this.productoRepository = productoRepository;
        this.proveedorProductoRepository = proveedorProductoRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public Pagina<FichaResumen> listar(int pagina, int tamano) {
        UUID organizacionId = organizacionService.organizacionActual();
        Map<UUID, BigDecimal> precios = mejoresPrecios(organizacionId);

        Pageable orden = Pagina.peticion(pagina, tamano, Sort.by("nombre").ascending());

        return Pagina.desde(
                        fichaRepository.findByOrganizacionIdAndDeletedAtIsNull(
                                organizacionId, orden)
                                .map(
                        ficha -> {
                            List<FichaLinea> lineas =
                                    fichaLineaRepository.findByFichaId(ficha.getId());
                            BigDecimal costeTotal = costeTotal(lineas, precios);
                            BigDecimal porRacion = porRacion(costeTotal, ficha.getRaciones());
                            return new FichaResumen(
                                    ficha.getId(),
                                    ficha.getNombre(),
                                    ficha.getRaciones(),
                                    ficha.getPrecioVenta(),
                                    porRacion,
                                    margen(ficha.getPrecioVenta(), porRacion),
                                    lineas.size());
                        }));
    }

    @Transactional(readOnly = true)
    public FichaDetalle obtener(UUID id) {
        Ficha ficha = buscar(id);
        return detalle(ficha, mejoresPrecios(ficha.getOrganizacionId()));
    }

    @Transactional
    public FichaDetalle crear(FichaRequest request) {
        Ficha ficha = new Ficha();
        ficha.setOrganizacionId(organizacionService.organizacionActual());
        aplicar(ficha, request);
        Ficha guardada = fichaRepository.save(ficha);
        sustituirLineas(guardada, request.lineas());
        return detalle(guardada, mejoresPrecios(guardada.getOrganizacionId()));
    }

    @Transactional
    public FichaDetalle actualizar(UUID id, FichaRequest request) {
        Ficha ficha = buscar(id);
        aplicar(ficha, request);
        fichaRepository.save(ficha);
        sustituirLineas(ficha, request.lineas());
        return detalle(ficha, mejoresPrecios(ficha.getOrganizacionId()));
    }

    @Transactional
    public void eliminar(UUID id) {
        Ficha ficha = buscar(id);
        ficha.setDeletedAt(OffsetDateTime.now());
        fichaRepository.save(ficha);
    }

    private void aplicar(Ficha ficha, FichaRequest request) {
        ficha.setNombre(request.nombre().trim());
        ficha.setDescripcion(request.descripcion());
        ficha.setRaciones(request.raciones() == null ? 1 : request.raciones());
        ficha.setMargenObjetivo(request.margenObjetivo());
        ficha.setPrecioVenta(request.precioVenta());
    }

    private void sustituirLineas(Ficha ficha, List<FichaLineaRequest> lineas) {
        List<FichaLinea> actuales = fichaLineaRepository.findByFichaId(ficha.getId());
        fichaLineaRepository.deleteAll(actuales);

        if (lineas == null) {
            return;
        }

        for (FichaLineaRequest peticion : lineas) {
            if (peticion.productoId() == null
                    && (peticion.descripcion() == null || peticion.descripcion().isBlank())) {
                continue;
            }

            FichaLinea linea = new FichaLinea();
            linea.setFicha(ficha);
            linea.setProducto(resolverProducto(peticion.productoId()));
            linea.setDescripcion(peticion.descripcion());
            linea.setCantidad(peticion.cantidad() == null ? BigDecimal.ZERO : peticion.cantidad());
            linea.setUnidad(peticion.unidad());
            fichaLineaRepository.save(linea);
        }
    }

    private FichaDetalle detalle(Ficha ficha, Map<UUID, BigDecimal> precios) {
        List<FichaLinea> lineas = fichaLineaRepository.findByFichaId(ficha.getId());

        int sinPrecio = 0;
        List<FichaLineaResponse> respuestas = new ArrayList<>();
        for (FichaLinea linea : lineas) {
            BigDecimal precio =
                    linea.getProducto() != null ? precios.get(linea.getProducto().getId()) : null;
            if (linea.getProducto() != null && precio == null) {
                sinPrecio++;
            }
            BigDecimal coste =
                    precio == null
                            ? null
                            : precio.multiply(linea.getCantidad()).setScale(2, RoundingMode.HALF_UP);

            respuestas.add(
                    new FichaLineaResponse(
                            linea.getId(),
                            linea.getProducto() != null ? linea.getProducto().getId() : null,
                            linea.getProducto() != null ? linea.getProducto().getNombre() : null,
                            linea.getDescripcion(),
                            linea.getCantidad(),
                            linea.getUnidad(),
                            precio,
                            coste));
        }

        BigDecimal costeTotal = costeTotal(lineas, precios);
        BigDecimal porRacion = porRacion(costeTotal, ficha.getRaciones());

        return new FichaDetalle(
                ficha.getId(),
                ficha.getNombre(),
                ficha.getDescripcion(),
                ficha.getRaciones(),
                ficha.getMargenObjetivo(),
                ficha.getPrecioVenta(),
                costeTotal,
                porRacion,
                margen(ficha.getPrecioVenta(), porRacion),
                sinPrecio,
                respuestas);
    }

    /** Mejor precio vigente (el mas bajo) de cada producto entre todos los proveedores. */
    private Map<UUID, BigDecimal> mejoresPrecios(UUID organizacionId) {
        Map<UUID, BigDecimal> precios = new HashMap<>();
        List<ProveedorProducto> catalogo =
                proveedorProductoRepository.findByOrganizacionId(organizacionId);

        for (ProveedorProducto item : catalogo) {
            if (item.getProducto() == null || item.getPrecioUltimo() == null) {
                continue;
            }
            UUID productoId = item.getProducto().getId();
            precios.merge(productoId, item.getPrecioUltimo(), (actual, nuevo) ->
                    actual.compareTo(nuevo) <= 0 ? actual : nuevo);
        }
        return precios;
    }

    private BigDecimal costeTotal(List<FichaLinea> lineas, Map<UUID, BigDecimal> precios) {
        return lineas.stream()
                .filter(linea -> linea.getProducto() != null)
                .filter(linea -> precios.containsKey(linea.getProducto().getId()))
                .map(
                        linea ->
                                precios
                                        .get(linea.getProducto().getId())
                                        .multiply(linea.getCantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal porRacion(BigDecimal costeTotal, Integer raciones) {
        int numero = raciones == null || raciones <= 0 ? 1 : raciones;
        return costeTotal.divide(BigDecimal.valueOf(numero), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal margen(BigDecimal precioVenta, BigDecimal costePorRacion) {
        if (precioVenta == null || precioVenta.signum() <= 0 || costePorRacion == null) {
            return null;
        }
        return precioVenta
                .subtract(costePorRacion)
                .multiply(CIEN)
                .divide(precioVenta, 2, RoundingMode.HALF_UP);
    }

    private Producto resolverProducto(UUID productoId) {
        if (productoId == null) {
            return null;
        }
        return productoRepository
                .findById(productoId)
                .filter(producto -> producto.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }

    private Ficha buscar(UUID id) {
        return fichaRepository
                .findById(id)
                .filter(ficha -> ficha.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Ficha no encontrada"));
    }
}
