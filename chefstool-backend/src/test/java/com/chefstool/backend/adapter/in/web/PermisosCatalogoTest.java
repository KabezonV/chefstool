package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Con la seguridad ACTIVA: el catalogo solo lo borran propietario y admin. */
class PermisosCatalogoTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void unMiembroTrabajaConElCatalogoPeroNoBorra() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es");
        String emailMiembro = "miembro-" + UUID.randomUUID() + "@cocina.es";

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", token(propietario))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"rol\":\"miembro\"}".formatted(emailMiembro)))
                .andExpect(status().isCreated());

        JsonNode miembro = registro(emailMiembro);

        // El miembro puede dar de alta y editar productos
        String productoId =
                objectMapper
                        .readTree(
                                mockMvc.perform(
                                                post("/api/productos")
                                                        .header("Authorization", token(miembro))
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content(
                                                                """
                                                                {"nombre":"Sal del miembro","unidadBase":"kg"}
                                                                """))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get("id")
                        .asString();

        // ... pero no borrarlos
        mockMvc.perform(
                        delete("/api/productos/" + productoId)
                                .header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());

        // El propietario si puede
        mockMvc.perform(
                        delete("/api/productos/" + productoId)
                                .header("Authorization", token(propietario)))
                .andExpect(status().isNoContent());
    }

    private String token(JsonNode sesion) {
        return "Bearer " + sesion.get("token").asString();
    }

    private JsonNode registro(String email) throws Exception {
        String respuesta =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"secreto123",
                                                 "nombre":"Chef","organizacion":"Cocina %s"}
                                                """
                                                        .formatted(email, UUID.randomUUID())))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return objectMapper.readTree(respuesta);
    }
}
