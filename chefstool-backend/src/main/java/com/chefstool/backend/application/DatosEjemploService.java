package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.dto.ClienteRequest;
import com.chefstool.backend.adapter.in.web.dto.ClienteResponse;
import com.chefstool.backend.adapter.in.web.dto.DocumentoDetalle;
import com.chefstool.backend.adapter.in.web.dto.EventoRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaLineaRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoResponse;
import com.chefstool.backend.adapter.in.web.dto.ProveedorRequest;
import com.chefstool.backend.adapter.in.web.dto.ProveedorResponse;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Siembra una organizacion recien creada con un juego de datos realista (catalogo,
 * proveedores, albaranes confirmados y fichas tecnicas) para que el panel no arranque vacio.
 */
@Service
public class DatosEjemploService {

    private final ProductoService productoService;
    private final ProveedorService proveedorService;
    private final ClienteService clienteService;
    private final EventoService eventoService;
    private final DocumentoCompraService documentoService;
    private final FichaService fichaService;
    private final ProductoRepository productoRepository;
    private final OrganizacionService organizacionService;

    public DatosEjemploService(
            ProductoService productoService,
            ProveedorService proveedorService,
            ClienteService clienteService,
            EventoService eventoService,
            DocumentoCompraService documentoService,
            FichaService fichaService,
            ProductoRepository productoRepository,
            OrganizacionService organizacionService) {
        this.productoService = productoService;
        this.proveedorService = proveedorService;
        this.clienteService = clienteService;
        this.eventoService = eventoService;
        this.documentoService = documentoService;
        this.fichaService = fichaService;
        this.productoRepository = productoRepository;
        this.organizacionService = organizacionService;
    }

    public record Resumen(
            int productos, int proveedores, int clientes, int eventos, int compras, int fichas) {}

    @Transactional
    public Resumen cargar() {
        UUID organizacionId = organizacionService.organizacionActual();
        if (!productoRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(organizacionId)
                .isEmpty()) {
            throw new ConflictoException(
                    "Esta organizacion ya tiene datos: vacia el catalogo antes de cargar el ejemplo");
        }

        ProductoResponse tomate = producto("Tomate pera", "Verdura", "kg");
        ProductoResponse cebolla = producto("Cebolla", "Verdura", "kg");
        ProductoResponse aceite = producto("Aceite de oliva", "Aceites", "l");
        ProductoResponse arroz = producto("Arroz bomba", "Secos", "kg");
        ProductoResponse solomillo = producto("Solomillo de ternera", "Carnes", "kg");
        ProductoResponse harina = producto("Harina de trigo", "Secos", "kg");
        ProductoResponse nata = producto("Nata liquida", "Lacteos", "l");
        producto("Sal fina", "Secos", "kg");

        ProveedorResponse frutas =
                proveedorService.crear(
                        new ProveedorRequest(
                                "Frutas Garcia", "B12345678", "pedidos@frutasgarcia.es", "600111222", null));
        ProveedorResponse carnicas =
                proveedorService.crear(
                        new ProveedorRequest(
                                "Carnicas Sanz", "B87654321", "ventas@carnicassanz.es", "600333444", null));
        proveedorService.crear(
                new ProveedorRequest(
                        "Distribuciones Levante", "B11223344", "info@dlevante.es", "600555666", null));

        ClienteResponse hotel =
                clienteService.crear(
                        new ClienteRequest("Hotel Marina", "eventos@hotelmarina.es", "600777888", null));
        ClienteResponse restaurante =
                clienteService.crear(
                        new ClienteRequest("Restaurante El Puerto", null, "600999000", null));

        LocalDate hoy = LocalDate.now();
        eventoService.crear(
                new EventoRequest("Boda Marina", hoy.plusDays(5), 120, hotel.id(), null));
        eventoService.crear(
                new EventoRequest(
                        "Menu del dia", hoy, 40, restaurante.id(), null));

        albaran(
                frutas,
                "ALB-DEMO-001",
                hoy.minusDays(20),
                List.of(
                        linea(tomate, 10, "kg", "2.10"),
                        linea(cebolla, 8, "kg", "1.10")));
        albaran(
                carnicas,
                "ALB-DEMO-002",
                hoy.minusDays(10),
                List.of(
                        linea(solomillo, 6, "kg", "18.50"),
                        linea(nata, 4, "l", "2.40")));
        // Segunda compra del tomate mas caro: dispara la alerta de subida de precio.
        albaran(
                frutas,
                "ALB-DEMO-003",
                hoy.minusDays(2),
                List.of(
                        linea(tomate, 12, "kg", "2.60"),
                        linea(aceite, 5, "l", "6.90")));

        ficha("Paella de marisco", 10, "18.50", List.of(
                lineaFicha(arroz, "1.2", "kg"),
                lineaFicha(tomate, "0.5", "kg"),
                lineaFicha(aceite, "0.3", "l")));
        ficha("Solomillo al Pedro Ximenez", 8, "22.00", List.of(
                lineaFicha(solomillo, "2.4", "kg"),
                lineaFicha(nata, "0.5", "l"),
                lineaFicha(harina, "0.2", "kg")));

        return new Resumen(8, 3, 2, 2, 3, 2);
    }

    private ProductoResponse producto(String nombre, String categoria, String unidad) {
        return productoService.crear(new ProductoRequest(nombre, categoria, unidad));
    }

    private LineaExtraccion linea(ProductoResponse producto, int cantidad, String unidad, String precio) {
        BigDecimal precioUnitario = new BigDecimal(precio);
        BigDecimal importe =
                precioUnitario.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
        return new LineaExtraccion(
                producto.nombre(),
                BigDecimal.valueOf(cantidad),
                unidad,
                precioUnitario,
                importe,
                producto.id(),
                null,
                null);
    }

    private void albaran(
            ProveedorResponse proveedor, String numero, LocalDate fecha, List<LineaExtraccion> lineas) {
        BigDecimal neto =
                lineas.stream()
                        .map(LineaExtraccion::importe)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal iva = neto.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);

        ExtraccionDocumento extraccion =
                new ExtraccionDocumento(
                        proveedor.nombre(),
                        proveedor.id(),
                        numero,
                        fecha,
                        "EUR",
                        neto,
                        iva,
                        neto.add(iva),
                        lineas);

        DocumentoDetalle documento = documentoService.subir(null, "demo");
        documentoService.confirmar(documento.id(), extraccion);
    }

    private FichaLineaRequest lineaFicha(ProductoResponse producto, String cantidad, String unidad) {
        return new FichaLineaRequest(
                producto.id(), producto.nombre(), new BigDecimal(cantidad), unidad);
    }

    private void ficha(String nombre, int raciones, String precioVenta, List<FichaLineaRequest> lineas) {
        fichaService.crear(
                new FichaRequest(
                        nombre, null, raciones, null, new BigDecimal(precioVenta), lineas));
    }
}
