package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.LineaCompra;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.port.out.LineaCompraRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.HistoricoPrecios;
import com.chefstool.backend.adapter.in.web.dto.PuntoPrecio;
import com.chefstool.backend.adapter.in.web.dto.PrecioProveedor;
import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoResponse;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.chefstool.backend.application.dto.Pagina;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final LineaCompraRepository lineaCompraRepository;
    private final OrganizacionService organizacionService;

    public ProductoService(
            ProductoRepository productoRepository,
            LineaCompraRepository lineaCompraRepository,
            OrganizacionService organizacionService) {
        this.productoRepository = productoRepository;
        this.lineaCompraRepository = lineaCompraRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public HistoricoPrecios historico(UUID productoId) {
        Producto producto =
                productoRepository
                        .findById(productoId)
                        .filter(p -> p.getDeletedAt() == null)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Producto no encontrado"));

        record Punto(UUID proveedorId, String proveedorNombre, java.time.LocalDate fecha, BigDecimal precio) {}

        List<Punto> puntos =
                lineaCompraRepository.historialPorProducto(productoId).stream()
                        .filter(linea -> linea.getCompra().getProveedor() != null)
                        .filter(linea -> precio(linea) != null)
                        .map(
                                linea ->
                                        new Punto(
                                                linea.getCompra().getProveedor().getId(),
                                                linea.getCompra().getProveedor().getNombre(),
                                                linea.getCompra().getFecha(),
                                                precio(linea)))
                        .toList();

        Map<UUID, List<Punto>> porProveedor =
                puntos.stream()
                        .collect(
                                Collectors.groupingBy(
                                        Punto::proveedorId,
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        List<PrecioProveedor> proveedores =
                porProveedor.values().stream()
                        .map(
                                lista -> {
                                    Punto actual = lista.get(lista.size() - 1);
                                    BigDecimal minimo =
                                            lista.stream()
                                                    .map(Punto::precio)
                                                    .min(BigDecimal::compareTo)
                                                    .orElse(null);
                                    BigDecimal maximo =
                                            lista.stream()
                                                    .map(Punto::precio)
                                                    .max(BigDecimal::compareTo)
                                                    .orElse(null);
                                    return new PrecioProveedor(
                                            actual.proveedorId(),
                                            actual.proveedorNombre(),
                                            actual.precio(),
                                            minimo,
                                            maximo,
                                            actual.fecha());
                                })
                        .sorted(Comparator.comparing(PrecioProveedor::precioActual))
                        .toList();

        PrecioProveedor mejor = proveedores.isEmpty() ? null : proveedores.get(0);

        List<PuntoPrecio> evolucion =
                puntos.stream()
                        .map(p -> new PuntoPrecio(p.fecha(), p.precio(), p.proveedorNombre()))
                        .toList();

        return new HistoricoPrecios(
                producto.getId(),
                producto.getNombre(),
                producto.getUnidadBase(),
                mejor != null ? mejor.proveedorNombre() : null,
                mejor != null ? mejor.precioActual() : null,
                proveedores,
                evolucion);
    }

    private BigDecimal precio(LineaCompra linea) {
        return linea.getPrecioNormalizado() != null
                ? linea.getPrecioNormalizado()
                : linea.getPrecioUnitario();
    }

    @Transactional(readOnly = true)
    public Pagina<ProductoResponse> listar(int pagina, int tamano, String busqueda) {
        UUID organizacionId = organizacionService.organizacionActual();
        Pageable orden = Pagina.peticion(pagina, tamano, Sort.by("nombre").ascending());

        Page<Producto> resultado =
                busqueda == null || busqueda.isBlank()
                        ? productoRepository.findByOrganizacionIdAndDeletedAtIsNull(
                                organizacionId, orden)
                        : productoRepository
                                .findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                                        organizacionId, busqueda.trim(), orden);

        return Pagina.desde(resultado).map(ProductoResponse::from);
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();
        comprobarNombreLibre(organizacionId, nombre);

        Producto producto = new Producto();
        producto.setOrganizacionId(organizacionId);
        producto.setNombre(nombre);
        aplicar(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(UUID id, ProductoRequest request) {
        Producto producto = buscar(id);
        String nombre = request.nombre().trim();

        if (!producto.getNombre().equalsIgnoreCase(nombre)) {
            comprobarNombreLibre(producto.getOrganizacionId(), nombre);
        }

        producto.setNombre(nombre);
        aplicar(producto, request);
        return ProductoResponse.from(productoRepository.save(producto));
    }

    @Transactional
    public void eliminar(UUID id) {
        Producto producto = buscar(id);
        producto.setDeletedAt(OffsetDateTime.now());
    }

    private void aplicar(Producto producto, ProductoRequest request) {
        producto.setCategoria(request.categoria());
        if (request.unidadBase() != null && !request.unidadBase().isBlank()) {
            producto.setUnidadBase(request.unidadBase());
        }
    }

    private void comprobarNombreLibre(UUID organizacionId, String nombre) {
        if (productoRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un producto llamado " + nombre);
        }
    }

    private Producto buscar(UUID id) {
        return productoRepository
                .findById(id)
                .filter(producto -> producto.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado"));
    }
}
