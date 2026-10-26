package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.application.CompraService;
import com.chefstool.backend.domain.model.DocumentoCompra;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.port.out.DocumentoCompraRepository;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
class InformePanelTest extends IntegracionBase {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompraService compraService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private DocumentoCompraRepository documentoRepository;

    @Test
    void informaDelCostePorProveedor() throws Exception {
        Producto producto = producto();
        Proveedor caro = proveedor();
        Proveedor barato = proveedor();

        compra(caro, producto, "10.00", LocalDate.now().minusDays(10));
        compra(caro, producto, "12.00", LocalDate.now().minusDays(2));
        compra(barato, producto, "5.00", LocalDate.now());

        JsonNode resumen =
                objectMapper.readTree(
                        mockMvc.perform(get("/api/informes/proveedores"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());

        JsonNode caroJson = null;
        JsonNode baratoJson = null;
        for (JsonNode nodo : resumen) {
            if (nodo.get("nombre").asString().equals(caro.getNombre())) {
                caroJson = nodo;
            }
            if (nodo.get("nombre").asString().equals(barato.getNombre())) {
                baratoJson = nodo;
            }
        }

        assertThat(caroJson).isNotNull();
        assertThat(baratoJson).isNotNull();

        // Dos compras: 2 x 10 = 20 de base + 10 % de IVA, y 2 x 12 = 24 + IVA
        assertThat(caroJson.get("compras").asInt()).isEqualTo(2);
        assertThat(new BigDecimal(caroJson.get("gastoTotal").asString()))
                .isEqualByComparingTo(new BigDecimal("48.40"));
        assertThat(new BigDecimal(caroJson.get("ticketMedio").asString()))
                .isEqualByComparingTo(new BigDecimal("24.20"));
        assertThat(caroJson.get("productos").asInt()).isEqualTo(1);
        // De 10,00 a 12,00 euros el kilo: +20 %
        assertThat(new BigDecimal(caroJson.get("variacionMediaPct").asString()))
                .isEqualByComparingTo(new BigDecimal("20.0"));

        assertThat(baratoJson.get("compras").asInt()).isEqualTo(1);
        // Un solo precio: no hay variacion que calcular
        assertThat(baratoJson.get("variacionMediaPct").isNull()).isTrue();

        // El ranking viene ordenado de mayor a menor gasto (la base de datos de test
        // puede tener compras de otras pruebas, asi que comparamos posiciones)
        List<String> nombres = new java.util.ArrayList<>();
        resumen.forEach(nodo -> nombres.add(nodo.get("nombre").asString()));
        assertThat(nombres.indexOf(caro.getNombre()))
                .isLessThan(nombres.indexOf(barato.getNombre()));
    }

    @Test
    void panelAgregaGastoAhorroYDocumentosPendientes() throws Exception {
        Producto producto = producto();
        Proveedor proveedor = proveedor();

        compra(proveedor, producto, "10.00", LocalDate.now().minusMonths(1));
        compra(proveedor, producto, "8.00", LocalDate.now());

        String documentoId =
                objectMapper
                        .readTree(
                                mockMvc.perform(multipart("/api/documentos").param("origen", "demo"))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get("id")
                        .asString();

        JsonNode panel =
                objectMapper.readTree(
                        mockMvc.perform(get("/api/informes/panel"))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString());

        JsonNode proveedorEnPanel =
                panel.get("gastoPorProveedor").valueStream()
                        .filter(nodo -> nodo.get("etiqueta").asString().equals(proveedor.getNombre()))
                        .findFirst()
                        .orElseThrow();

        assertThat(proveedorEnPanel.get("importe").asDouble()).isEqualTo(17.60);
        assertThat(panel.get("ahorroPotencial").asDouble()).isGreaterThanOrEqualTo(4.00);
        assertThat(panel.get("lineasMejorables").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(panel.get("comprasAnalizadas").asInt()).isGreaterThanOrEqualTo(2);
        assertThat(panel.get("gastoPorMes")).hasSize(6);
        assertThat(panel.get("kpis").get("documentosPendientes").asInt())
                .isGreaterThanOrEqualTo(1);
        assertThat(
                        panel.get("documentosPorRevisar").valueStream()
                                .map(nodo -> nodo.get("id").asString())
                                .toList())
                .contains(documentoId);
    }

    private void compra(
            Proveedor proveedor, Producto producto, String precio, LocalDate fecha) {
        BigDecimal precioUnitario = new BigDecimal(precio);
        BigDecimal importe = precioUnitario.multiply(BigDecimal.valueOf(2));
        BigDecimal iva = importe.multiply(new BigDecimal("0.10"));

        ExtraccionDocumento extraccion =
                new ExtraccionDocumento(
                        proveedor.getNombre(),
                        proveedor.getId(),
                        "ALB-" + UUID.randomUUID().toString().substring(0, 6),
                        fecha,
                        "EUR",
                        importe,
                        iva,
                        importe.add(iva),
                        List.of(
                                new LineaExtraccion(
                                        producto.getNombre(),
                                        BigDecimal.valueOf(2),
                                        "kg",
                                        precioUnitario,
                                        importe,
                                        producto.getId(),
                                        null,
                                        null)));

        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(ORG);
        documento.setNombreOriginal("albaran.pdf");

        compraService.crearDesdeDocumento(documentoRepository.save(documento), extraccion);
    }

    private Producto producto() {
        Producto producto = new Producto();
        producto.setOrganizacionId(ORG);
        producto.setNombre("Panel producto " + UUID.randomUUID());
        producto.setUnidadBase("kg");
        return productoRepository.save(producto);
    }

    private Proveedor proveedor() {
        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(ORG);
        proveedor.setNombre("Panel proveedor " + UUID.randomUUID());
        return proveedorRepository.save(proveedor);
    }
}
