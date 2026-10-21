package com.chefstool.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void listaProductos() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void creaYActualizaProducto() throws Exception {
        String nombre = "Producto " + UUID.randomUUID();
        String id = crear(nombre, "kg");

        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].nombre").value(nombre));

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
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").isEmpty());
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
}
