package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
class ProductoControllerTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void listaProductosPaginado() throws Exception {
        mockMvc.perform(get("/api/productos").param("pagina", "0").param("tamano", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(2))
                .andExpect(jsonPath("$.totalElementos").isNumber())
                .andExpect(jsonPath("$.totalPaginas").isNumber());
    }

    @Test
    void buscaProductosPorNombre() throws Exception {
        String nombre = "Busqueda " + UUID.randomUUID();
        crear(nombre, "kg");

        mockMvc.perform(get("/api/productos").param("q", nombre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.nombre=='" + nombre + "')]").isNotEmpty())
                .andExpect(jsonPath("$.contenido[?(@.nombre=='No existe " + nombre + "')]").isEmpty());

        mockMvc.perform(get("/api/productos").param("q", "no-existe-" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void ordenaPorNombreEnAmbosSentidos() throws Exception {
        crear("Aaa " + UUID.randomUUID(), "kg");
        crear("Zzz " + UUID.randomUUID(), "kg");

        String ascendente =
                mockMvc.perform(get("/api/productos").param("orden", "nombre").param("direccion", "asc"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String descendente =
                mockMvc.perform(
                                get("/api/productos")
                                        .param("orden", "nombre")
                                        .param("direccion", "desc"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        var asc = nombres(ascendente);
        var desc = nombres(descendente);
        assertThat(desc).isEqualTo(asc.reversed());
    }

    @Test
    void ignoraOrdenNoPermitido() throws Exception {
        mockMvc.perform(get("/api/productos").param("orden", "drop table productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isArray());
    }

    @Test
    void creaYActualizaProducto() throws Exception {
        String nombre = "Producto " + UUID.randomUUID();
        String id = crear(nombre, "kg");

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.id=='" + id + "')].nombre").value(nombre));

        String nuevoNombre = "Renombrado " + UUID.randomUUID();
        mockMvc.perform(
                        put("/api/productos/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre":"%s","unidadBase":"l"}
                                        """
                                                .formatted(nuevoNombre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(nuevoNombre))
                .andExpect(jsonPath("$.unidadBase").value("l"));
    }

    @Test
    void eliminaProductoYDesapareceDelListado() throws Exception {
        String id = crear("Temporal " + UUID.randomUUID(), "ud");

        mockMvc.perform(delete("/api/productos/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[?(@.id=='" + id + "')]").isEmpty());
    }

    @Test
    void productoEliminadoDevuelve404AlActualizar() throws Exception {
        String id = crear("Borrado " + UUID.randomUUID(), "ud");
        mockMvc.perform(delete("/api/productos/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(
                        put("/api/productos/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"Otra vez\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaProductoSinNombre() throws Exception {
        mockMvc.perform(
                        post("/api/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }


    @Test
    void permiteRecrearUnProductoBorradoConElMismoNombre() throws Exception {
        String nombre = "Reciclado " + UUID.randomUUID();
        String id = crear(nombre, "kg");
        mockMvc.perform(delete("/api/productos/" + id)).andExpect(status().isNoContent());

        crear(nombre, "kg");
    }

    private String crear(String nombre, String unidad) throws Exception {
        String cuerpo =
                """
                {"nombre":"%s","categoria":"Secos","unidadBase":"%s"}
                """
                        .formatted(nombre, unidad);

        String respuesta =
                mockMvc.perform(
                                post("/api/productos")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.nombre").value(nombre))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asText();
    }

    private List<String> nombres(String cuerpo) throws Exception {
        List<String> nombres = new ArrayList<>();
        for (var nodo : objectMapper.readTree(cuerpo).get("contenido")) {
            nombres.add(nodo.get("nombre").asString());
        }
        return nombres;
    }
}
