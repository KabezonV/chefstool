package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.IntegracionBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Auditoría y locales: solo propietario y admin los gestionan; el miembro solo consulta. */
class AuditoriaYLocalesTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void laAuditoriaDiceQuienHizoQue() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Marta Jefa");

        mockMvc.perform(
                        post("/api/productos")
                                .header("Authorization", token(propietario))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"Sal auditada\",\"unidadBase\":\"kg\"}"))
                .andExpect(status().isCreated());

        String respuesta = mockMvc.perform(
                        get("/api/auditoria").header("Authorization", token(propietario)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode eventos = objectMapper.readTree(respuesta).get("contenido");
        assertThat(eventos.size()).isGreaterThan(0);
        JsonNode evento = eventos.get(0);
        assertThat(evento.get("accion").asString()).isNotBlank();
        assertThat(evento.get("entidad").asString()).isNotBlank();
        assertThat(evento.get("usuarioNombre").asString()).isEqualTo("Marta Jefa");
    }

    @Test
    void unMiembroNoVeLaAuditoria() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefe");
        String emailMiembro = "miembro-" + UUID.randomUUID() + "@cocina.es";
        invitar(propietario, emailMiembro, "miembro");
        JsonNode miembro = registro(emailMiembro, "Cocinero");

        mockMvc.perform(get("/api/auditoria").header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());
    }

    @Test
    void elPropietarioCreaYListaLocales() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefe");
        String nombre = "Terraza " + UUID.randomUUID().toString().substring(0, 6);

        String creado = mockMvc.perform(
                        post("/api/locales")
                                .header("Authorization", token(propietario))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"%s\"}".formatted(nombre)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String localId = objectMapper.readTree(creado).get("id").asString();

        mockMvc.perform(get("/api/locales").header("Authorization", token(propietario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + localId + "')].nombre").value(nombre));

        mockMvc.perform(
                        post("/api/locales")
                                .header("Authorization", token(propietario))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"%s\"}".formatted(nombre)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(
                        delete("/api/locales/" + localId)
                                .header("Authorization", token(propietario)))
                .andExpect(status().isNoContent());

        String restantes = mockMvc.perform(
                        get("/api/locales").header("Authorization", token(propietario)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(restantes).doesNotContain(localId);
    }

    @Test
    void unMiembroNoPuedeCrearNiBorrarLocales() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefe");
        String emailMiembro = "miembro-" + UUID.randomUUID() + "@cocina.es";
        invitar(propietario, emailMiembro, "miembro");
        JsonNode miembro = registro(emailMiembro, "Cocinero");

        mockMvc.perform(
                        post("/api/locales")
                                .header("Authorization", token(miembro))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"Cocina del miembro\"}"))
                .andExpect(status().isForbidden());

        String locales = mockMvc.perform(
                        get("/api/locales").header("Authorization", token(propietario)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String localId = objectMapper.readTree(locales).get(0).get("id").asString();

        mockMvc.perform(delete("/api/locales/" + localId).header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());
    }

    private void invitar(JsonNode propietario, String email, String rol) throws Exception {
        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", token(propietario))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"rol\":\"%s\"}".formatted(email, rol)))
                .andExpect(status().isCreated());
    }

    private String token(JsonNode sesion) {
        return "Bearer " + sesion.get("token").asString();
    }

    private JsonNode registro(String email, String nombre) throws Exception {
        String respuesta = mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"email":"%s","password":"secreto123",
                                         "nombre":"%s","organizacion":"Cocina %s"}
                                        """
                                        .formatted(email, nombre, UUID.randomUUID().toString()
                                                .substring(0, 6))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(respuesta);
    }
}
