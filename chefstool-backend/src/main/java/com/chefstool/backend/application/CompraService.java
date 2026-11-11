package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.AlertaPrecio;
import com.chefstool.backend.domain.model.Cliente;
import com.chefstool.backend.domain.model.Compra;
import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.model.Evento;
import com.chefstool.backend.domain.model.LineaCompra;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ProveedorProducto;
import com.chefstool.backend.domain.port.out.AlertaPrecioRepository;
import com.chefstool.backend.domain.port.out.ClienteRepository;
import com.chefstool.backend.domain.port.out.CompraRepository;
import com.chefstool.backend.domain.port.out.EventoRepository;
import com.chefstool.backend.domain.port.out.LineaCompraRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.CompraDetalle;
import com.chefstool.backend.application.dto.Pagina;
import com.chefstool.backend.adapter.in.web.dto.CompraResponse;
import com.chefstool.backend.adapter.in.web.dto.CompraResumen;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.adapter.in.web.dto.LineaCompraResponse;
import com.chefstool.backend.domain.model.LineaExtraccion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Convierte un documento revisado en una compra con sus lineas y actualiza precios. */
@Service
public class CompraService {

    private static final BigDecimal UMBRAL_ALERTA = new BigDecimal("5.00");
    private static final BigDecimal UMBRAL_AVISO = new BigDecimal("10.00");
    private static final BigDecimal UMBRAL_CRITICO = new BigDecimal("20.00");

    private final CompraRepository compraRepository;
    private final LineaCompraRepository lineaCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final EventoRepository eventoRepository;
    private final ProveedorProductoRepository proveedorProductoRepository;
    private final AlertaPrecioRepository alertaPrecioRepository;
    private final OrganizacionService organizacionService;

    public CompraService(
            CompraRepository compraRepository,
            LineaCompraRepository lineaCompraRepository,
            ProveedorRepository proveedorRepository,
            ProductoRepository productoRepository,
            ClienteRepository clienteRepository,
            EventoRepository eventoRepository,
            ProveedorProductoRepository proveedorProductoRepository,
            AlertaPrecioRepository alertaPrecioRepository,
            OrganizacionService organizacionService) {
        this.compraRepository = compraRepository;
        this.lineaCompraRepository = lineaCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.eventoRepository = eventoRepository;
        this.proveedorProductoRepository = proveedorProductoRepository;
        this.alertaPrecioRepository = alertaPrecioRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public Pagina<CompraResumen> listar(
            UUID proveedorId,
            LocalDate desde,
            LocalDate hasta,
            int pagina,
            int tamano,
            String orden,
            String direccion) {
        Pageable pageable =
                Pagina.peticion(
                        pagina,
                        tamano,
                        Pagina.orden(
                                orden,
                                direccion,
                                Sort.by("fecha").descending().and(Sort.by("createdAt").descending()),
                                "fecha", "importeTotal", "numeroDocumento", "createdAt"));

        return Pagina.desde(
                        compraRepository.buscarPaginado(
                                organizacionService.organizacionActual(),
                                proveedorId,
                                desde,
                                hasta,
                                pageable))
                .map(CompraResumen::from);
    }

    @Transactional(readOnly = true)
    public CompraDetalle obtener(UUID id) {
        Compra compra =
                compraRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Compra no encontrada"));
        List<LineaCompraResponse> lineas =
                lineaCompraRepository.findByCompraId(id).stream()
                        .map(LineaCompraResponse::from)
                        .toList();
        return CompraDetalle.from(compra, lineas);
    }

    @Transactional
    public CompraResponse crearDesdeDocumento(
            DocumentoCompra documento, ExtraccionDocumento extraccion) {
        Compra compra = new Compra();
        compra.setOrganizacionId(documento.getOrganizacionId());
        compra.setDocumento(documento);
        compra.setProveedor(resolverProveedor(documento.getOrganizacionId(), extraccion));
        compra.setNumeroDocumento(extraccion.numeroDocumento());
        compra.setFecha(extraccion.fecha() != null ? extraccion.fecha() : LocalDate.now());
        compra.setMoneda(
                extraccion.moneda() == null || extraccion.moneda().isBlank()
                        ? "EUR"
                        : extraccion.moneda());
        compra.setImporteNeto(cero(extraccion.importeNeto()));
        compra.setImporteIva(cero(extraccion.importeIva()));
        compra.setImporteTotal(cero(extraccion.importeTotal()));

        Compra guardada = compraRepository.save(compra);

        List<LineaExtraccion> lineas = extraccion.lineas() == null ? List.of() : extraccion.lineas();
        for (LineaExtraccion linea : lineas) {
            LineaCompra entidad = new LineaCompra();
            entidad.setCompra(guardada);
            entidad.setDescripcionOriginal(linea.descripcion());
            entidad.setCantidad(cero(linea.cantidad()));
            entidad.setUnidad(linea.unidad());
            entidad.setPrecioUnitario(cero(linea.precioUnitario()));
            entidad.setImporte(cero(linea.importe()));

            Producto producto = resolverProducto(linea.productoId());
            entidad.setProducto(producto);
            entidad.setCliente(resolverCliente(linea.clienteId()));
            entidad.setEvento(resolverEvento(linea.eventoId()));
            entidad.setPrecioNormalizado(precioNormalizado(producto, linea));

            LineaCompra lineaGuardada = lineaCompraRepository.save(entidad);
            actualizarPrecioYAlertas(guardada, lineaGuardada, producto);
        }

        return CompraResponse.from(guardada, lineas.size());
    }

    private void actualizarPrecioYAlertas(
            Compra compra, LineaCompra linea, Producto producto) {
        Proveedor proveedor = compra.getProveedor();
        if (producto == null || proveedor == null) {
            return;
        }

        BigDecimal precio =
                linea.getPrecioNormalizado() != null
                        ? linea.getPrecioNormalizado()
                        : linea.getPrecioUnitario();
        if (precio == null || precio.signum() <= 0) {
            return;
        }

        ProveedorProducto catalogo =
                proveedorProductoRepository
                        .findByProveedorIdAndProductoId(proveedor.getId(), producto.getId())
                        .orElseGet(
                                () -> {
                                    ProveedorProducto nuevo = new ProveedorProducto();
                                    nuevo.setOrganizacionId(compra.getOrganizacionId());
                                    nuevo.setProveedor(proveedor);
                                    nuevo.setProducto(producto);
                                    return nuevo;
                                });

        BigDecimal anterior = catalogo.getPrecioUltimo();
        if (anterior != null && anterior.signum() > 0 && precio.compareTo(anterior) > 0) {
            BigDecimal porcentaje =
                    precio.subtract(anterior)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(anterior, 2, RoundingMode.HALF_UP);

            if (porcentaje.compareTo(UMBRAL_ALERTA) >= 0) {
                AlertaPrecio alerta = new AlertaPrecio();
                alerta.setOrganizacionId(compra.getOrganizacionId());
                alerta.setProducto(producto);
                alerta.setProveedor(proveedor);
                alerta.setPrecioAnterior(anterior);
                alerta.setPrecioNuevo(precio);
                alerta.setPorcentajeCambio(porcentaje);
                alerta.setSeveridad(severidad(porcentaje));
                alerta.setMensaje(
                        "El precio de %s sube un %s %% (de %s a %s €)"
                                .formatted(
                                        producto.getNombre(),
                                        porcentaje.toPlainString(),
                                        anterior.toPlainString(),
                                        precio.toPlainString()));
                alertaPrecioRepository.save(alerta);
            }
        }

        catalogo.setNombreEnProveedor(linea.getDescripcionOriginal());
        catalogo.setUnidad(linea.getUnidad());
        catalogo.setPrecioUltimo(precio);
        catalogo.setFechaUltimaCompra(compra.getFecha());
        proveedorProductoRepository.save(catalogo);
    }

    private String severidad(BigDecimal porcentaje) {
        if (porcentaje.compareTo(UMBRAL_CRITICO) >= 0) {
            return "critico";
        }
        return porcentaje.compareTo(UMBRAL_AVISO) >= 0 ? "aviso" : "info";
    }

    private BigDecimal precioNormalizado(Producto producto, LineaExtraccion linea) {
        if (producto == null || linea.unidad() == null || linea.precioUnitario() == null) {
            return null;
        }
        return producto.getUnidadBase().equalsIgnoreCase(linea.unidad())
                ? linea.precioUnitario()
                : null;
    }

    private Proveedor resolverProveedor(UUID organizacionId, ExtraccionDocumento extraccion) {
        if (extraccion.proveedorId() != null) {
            return proveedorRepository
                    .findById(extraccion.proveedorId())
                    .filter(proveedor -> proveedor.getDeletedAt() == null)
                    .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado"));
        }
        String nombre = nombreDeProveedorUtilizable(extraccion.proveedorNombre());
        if (nombre == null) {
            return null;
        }
        return proveedorRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(organizacionId)
                .stream()
                .filter(p -> p.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElseGet(() -> crearProveedor(organizacionId, nombre));
    }

    /**
     * Normaliza el nombre leido del albaran y descarta lo que no parece un proveedor real, para
     * no llenar el catalogo con ruido del OCR ("1", "||", "N").
     */
    private String nombreDeProveedorUtilizable(String nombre) {
        if (nombre == null) {
            return null;
        }
        String limpio = nombre.trim().replaceAll("\\s+", " ");
        boolean tieneLetras = limpio.chars().anyMatch(Character::isLetter);
        return tieneLetras && limpio.length() >= 3 ? limpio : null;
    }

    private Proveedor crearProveedor(UUID organizacionId, String nombre) {
        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(organizacionId);
        proveedor.setNombre(nombre);
        return proveedorRepository.save(proveedor);
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

    private Cliente resolverCliente(UUID clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteRepository
                .findById(clienteId)
                .filter(cliente -> cliente.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    private Evento resolverEvento(UUID eventoId) {
        if (eventoId == null) {
            return null;
        }
        return eventoRepository
                .findById(eventoId)
                .filter(evento -> evento.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }

    private BigDecimal cero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }
}
