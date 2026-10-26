package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Corre con la seguridad ACTIVA: la siembra exige rol propietario o admin. */
class DatosEjemploControllerTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void cargaDatosDeEjemploConComprasYAlertas() throws Exception {
        String token = propietario();

        mockMvc.perform(post("/api/demo/datos").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productos").value(8))
                .andExpect(jsonPath("$.compras").value(3));

        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(8));

        mockMvc.perform(get("/api/compras").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(3));

        // El tomate se compra dos veces con una subida superior al 5 %.
        mockMvc.perform(get("/api/alertas").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.productoNombre=='Tomate pera')]").isNotEmpty());
    }

    @Test
    void noDuplicaDatosSiYaExisten() throws Exception {
        String token = propietario();

        mockMvc.perform(post("/api/demo/datos").header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/demo/datos").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void requiereAutenticacion() throws Exception {
        mockMvc.perform(post("/api/demo/datos")).andExpect(status().isUnauthorized());
    }

    private String propietario() throws Exception {
        String respuesta =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"demo-%s@cocina.es","password":"secreto123",
                                                 "nombre":"Chef","organizacion":"Cocina %s"}
                                                """
                                                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        JsonNode sesion = objectMapper.readTree(respuesta);
        return sesion.get("token").asString();
    }
}
