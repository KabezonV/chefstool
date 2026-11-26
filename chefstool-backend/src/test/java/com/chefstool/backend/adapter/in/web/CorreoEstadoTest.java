package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** El correo se puede diagnosticar aunque no haya credenciales SMTP. */
class CorreoEstadoTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void sinConfiguracionExplicaQueFalta() throws Exception {
        JsonNode jefe = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefa");

        String estado = mockMvc.perform(
                        get("/api/correo/estado").header("Authorization", token(jefe)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(estado);
        assertThat(json.get("disponible").asBoolean()).isFalse();
        assertThat(json.get("falta").size()).isGreaterThan(0);
    }

    @Test
    void laPruebaExplicaqueNoEstaConfiguradoEnVezDeFallar() throws Exception {
        JsonNode jefe = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefa");

        String respuesta = mockMvc.perform(
                        post("/api/correo/prueba").header("Authorization", token(jefe)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(respuesta);
        assertThat(json.get("enviado").asBoolean()).isFalse();
        assertThat(json.get("mensaje").asString()).contains("no esta configurado");
    }

    @Test
    void unMiembroNoPuedeVerNiProbarElCorreo() throws Exception {
        JsonNode jefe = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Jefe");
        String emailMiembro = "miembro-" + UUID.randomUUID() + "@cocina.es";
        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", token(jefe))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"rol\":\"miembro\"}".formatted(emailMiembro)))
                .andExpect(status().isCreated());
        JsonNode miembro = registro(emailMiembro, "Cocinero");

        mockMvc.perform(get("/api/correo/estado").header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/correo/prueba").header("Authorization", token(miembro)))
                .andExpect(status().isForbidden());
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
