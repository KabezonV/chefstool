package com.chefstool.backend.adapter.in.web;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        properties = "chefstool.almacenamiento.directorio=${java.io.tmpdir}/chefstool-test-uploads")
class InformePanelTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompraService compraService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private DocumentoCompraRepository documentoRepository;

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
