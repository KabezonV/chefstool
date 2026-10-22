package com.chefstool.backend.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ProveedorProducto;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FichaControllerTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private ProveedorProductoRepository proveedorProductoRepository;

    private Producto conPrecio;
    private Producto sinPrecio;

    @BeforeEach
    void setUp() {
        conPrecio = producto("Harina test");
        sinPrecio = producto("Especia test");

        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(ORG);
        proveedor.setNombre("Proveedor " + UUID.randomUUID().toString().substring(0, 6));
        proveedor = proveedorRepository.save(proveedor);

        ProveedorProducto catalogo = new ProveedorProducto();
        catalogo.setOrganizacionId(ORG);
        catalogo.setProveedor(proveedor);
        catalogo.setProducto(conPrecio);
        catalogo.setUnidad("kg");
        catalogo.setPrecioUltimo(new BigDecimal("10.00"));
        proveedorProductoRepository.save(catalogo);
    }

    @Test
    void calculaCosteYMargenDeLaFicha() throws Exception {
        mockMvc.perform(
                        post("/api/fichas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(peticion("Pan de prueba", 4, "12.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costeTotal").value(20.00))
                .andExpect(jsonPath("$.costePorRacion").value(5.00))
                .andExpect(jsonPath("$.margenPct").value(58.33))
                .andExpect(jsonPath("$.productosSinPrecio").value(1))
                .andExpect(jsonPath("$.lineas.length()").value(2))
                .andExpect(jsonPath("$.lineas[0].precioUnitario").value(10.00));
    }

    @Test
    void listaFichasConSuCoste() throws Exception {
        crear("Ficha listada", 2, "10.00");

        mockMvc.perform(get("/api/fichas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.nombre=='Ficha listada')].costePorRacion").value(10.00));
    }

    @Test
    void actualizaLineasYRecalcula() throws Exception {
        String id = crear("Ficha editable", 4, "12.00");

        String cuerpo =
                """
                {"nombre":"Ficha editable","raciones":2,"precioVenta":"20.00","lineas":[
                  {"productoId":"%s","cantidad":4,"unidad":"kg"}]}
                """
                        .formatted(conPrecio.getId());

        mockMvc.perform(
                        put("/api/fichas/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(1))
                .andExpect(jsonPath("$.costeTotal").value(40.00))
                .andExpect(jsonPath("$.costePorRacion").value(20.00))
                .andExpect(jsonPath("$.margenPct").value(0.00));
    }

    @Test
    void eliminaFichaYDevuelve404() throws Exception {
        String id = crear("Ficha temporal", 1, "5.00");

        mockMvc.perform(delete("/api/fichas/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/fichas/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void rechazaFichaSinNombre() throws Exception {
        mockMvc.perform(
                        post("/api/fichas")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private String crear(String nombre, int raciones, String precioVenta) throws Exception {
        String respuesta =
                mockMvc.perform(
                                post("/api/fichas")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(peticion(nombre, raciones, precioVenta)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asString();
    }

    private String peticion(String nombre, int raciones, String precioVenta) {
        return """
               {"nombre":"%s","descripcion":"Prueba","raciones":%d,"precioVenta":"%s",
                "lineas":[
                  {"productoId":"%s","cantidad":2,"unidad":"kg"},
                  {"productoId":"%s","cantidad":1,"unidad":"ud"}]}
               """
                .formatted(
                        nombre,
                        raciones,
                        precioVenta,
                        conPrecio.getId(),
                        sinPrecio.getId());
    }

    private Producto producto(String nombre) {
        Producto producto = new Producto();
        producto.setOrganizacionId(ORG);
        producto.setNombre(nombre + " " + UUID.randomUUID().toString().substring(0, 6));
        producto.setUnidadBase("kg");
        return productoRepository.save(producto);
    }
}
