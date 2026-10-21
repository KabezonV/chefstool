package com.chefstool.backend.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.domain.DocumentoCompra;
import com.chefstool.backend.domain.Producto;
import com.chefstool.backend.domain.Proveedor;
import com.chefstool.backend.repository.DocumentoCompraRepository;
import com.chefstool.backend.repository.ProductoRepository;
import com.chefstool.backend.repository.ProveedorRepository;
import com.chefstool.backend.service.CompraService;
import com.chefstool.backend.web.dto.CompraResponse;
import com.chefstool.backend.web.dto.ExtraccionDocumento;
import com.chefstool.backend.web.dto.LineaExtraccion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ComprasYPreciosTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CompraService compraService;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private DocumentoCompraRepository documentoRepository;

    private Producto producto;
    private Proveedor caro;
    private Proveedor barato;

    @BeforeEach
    void setUp() {
        producto = producto();
        caro = proveedor("Mayorista Caro");
        barato = proveedor("Mayorista Barato");
    }

    @Test
    void listaComprasYFiltraPorProveedor() throws Exception {
        CompraResponse primera = compra(caro, "10.00", LocalDate.now().minusDays(10));
        CompraResponse segunda = compra(caro, "12.00", LocalDate.now().minusDays(5));
        CompraResponse tercera = compra(barato, "9.00", LocalDate.now().minusDays(2));

        var todas = json(get("/api/compras"));
        assertThat(ids(todas)).contains(primera.id().toString(), segunda.id().toString(), tercera.id().toString());

        var delCaro = json(get("/api/compras").param("proveedorId", caro.getId().toString()));
        assertThat(delCaro).hasSize(2);
        assertThat(delCaro.get(0).get("proveedorNombre").asString()).isEqualTo(caro.getNombre());
    }

    @Test
    void detalleDeCompraIncluyeLineas() throws Exception {
        CompraResponse compra = compra(barato, "9.00", LocalDate.now());

        mockMvc.perform(get("/api/compras/" + compra.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proveedorNombre").value(barato.getNombre()))
                .andExpect(jsonPath("$.lineas.length()").value(1))
                .andExpect(jsonPath("$.lineas[0].productoNombre").value(producto.getNombre()));
    }

    @Test
    void historicoMuestraMejorProveedorYComparativa() throws Exception {
        compra(caro, "10.00", LocalDate.now().minusDays(10));
        compra(caro, "12.00", LocalDate.now().minusDays(5));
        compra(barato, "9.00", LocalDate.now().minusDays(2));

        mockMvc.perform(get("/api/productos/" + producto.getId() + "/precios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unidadBase").value("kg"))
                .andExpect(jsonPath("$.mejorProveedor").value(barato.getNombre()))
                .andExpect(jsonPath("$.mejorPrecio").value(9.00))
                .andExpect(jsonPath("$.proveedores.length()").value(2))
                .andExpect(jsonPath("$.proveedores[0].proveedorNombre").value(barato.getNombre()))
                .andExpect(jsonPath("$.proveedores[1].precioActual").value(12.00))
                .andExpect(jsonPath("$.proveedores[1].precioMinimo").value(10.00))
                .andExpect(jsonPath("$.evolucion.length()").value(3));
    }

    @Test
    void alertaDeSubidaSePuedeReconocer() throws Exception {
        compra(caro, "10.00", LocalDate.now().minusDays(10));
        compra(caro, "12.00", LocalDate.now().minusDays(5));

        var alertas =
                json(get("/api/alertas")).valueStream()
                        .filter(a -> producto.getNombre().equals(a.get("productoNombre").asString()))
                        .toList();

        assertThat(alertas).hasSize(1);
        var alerta = alertas.get(0);
        assertThat(alerta.get("severidad").asString()).isEqualTo("critico");
        assertThat(alerta.get("porcentajeCambio").asDouble()).isEqualTo(20.00);

        String alertaId = alerta.get("id").asString();
        mockMvc.perform(post("/api/alertas/" + alertaId + "/reconocer"))
                .andExpect(status().isNoContent());

        assertThat(ids(json(get("/api/alertas")))).doesNotContain(alertaId);
    }

    @Test
    void productoInexistenteEnHistoricoDevuelve404() throws Exception {
        mockMvc.perform(get("/api/productos/00000000-0000-0000-0000-0000000000ff/precios"))
                .andExpect(status().isNotFound());
    }

    private tools.jackson.databind.JsonNode json(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder peticion)
            throws Exception {
        String cuerpo =
                mockMvc.perform(peticion)
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(cuerpo);
    }

    private java.util.List<String> ids(tools.jackson.databind.JsonNode nodos) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        nodos.forEach(nodo -> ids.add(nodo.get("id").asString()));
        return ids;
    }

    private CompraResponse compra(Proveedor proveedor, String precio, LocalDate fecha) {
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

        return compraService.crearDesdeDocumento(documento(), extraccion);
    }

    private Producto producto() {
        Producto creado = new Producto();
        creado.setOrganizacionId(ORG);
        creado.setNombre("Producto " + UUID.randomUUID());
        creado.setUnidadBase("kg");
        return productoRepository.save(creado);
    }

    private Proveedor proveedor(String nombre) {
        Proveedor creado = new Proveedor();
        creado.setOrganizacionId(ORG);
        creado.setNombre(nombre + " " + UUID.randomUUID().toString().substring(0, 6));
        return proveedorRepository.save(creado);
    }

    private DocumentoCompra documento() {
        DocumentoCompra documento = new DocumentoCompra();
        documento.setOrganizacionId(ORG);
        documento.setNombreOriginal("albaran.pdf");
        return documentoRepository.save(documento);
    }
}
