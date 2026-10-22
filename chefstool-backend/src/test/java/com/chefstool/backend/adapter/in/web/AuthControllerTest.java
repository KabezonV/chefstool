package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Este test corre con la seguridad ACTIVA (sin perfil "test"). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registroCreaCuentaYDevuelveToken() throws Exception {
        String email = "chef-" + UUID.randomUUID() + "@chefstool.es";

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","password":"secreto123",
                                         "nombre":"Chef Prueba","organizacion":"Cocina Prueba"}
                                        """
                                                .formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("propietario"))
                .andExpect(jsonPath("$.organizacion").value("Cocina Prueba"));
    }

    @Test
    void noPermiteRegistrarDosVecesElMismoCorreo() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@chefstool.es";
        String cuerpo =
                "{\"email\":\"%s\",\"password\":\"secreto123\"}".formatted(email);

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginDevuelveTokenYRechazaContrasenaIncorrecta() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@chefstool.es";
        String clave = "secreto123";

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"%s\",\"password\":\"%s\"}"
                                                .formatted(email, clave)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"%s\",\"password\":\"%s\"}"
                                                .formatted(email, clave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"%s\",\"password\":\"otra-cosa\"}"
                                                .formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bloqueaTrasCincoIntentosFallidos() throws Exception {
        String email = "fuerza-bruta-" + UUID.randomUUID() + "@chefstool.es";

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"password\":\"secreto123\"}"
                                        .formatted(email)))
                .andExpect(status().isCreated());

        for (int intento = 0; intento < 5; intento++) {
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"email\":\"%s\",\"password\":\"mal\"}"
                                            .formatted(email)))
                    .andExpect(status().isUnauthorized());
        }

        // El sexto intento, incluso con la contrasena correcta, queda frenado
        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"password\":\"secreto123\"}"
                                        .formatted(email)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.containsString("Demasiados intentos")));
    }

    @Test
    void sinTokenNoSePuedeAccederALaApi() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/informes/panel")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenSeAccedeALaOrganizacionDelUsuario() throws Exception {
        String email = "org-" + UUID.randomUUID() + "@chefstool.es";

        MvcResult registro =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"secreto123",
                                                 "nombre":"Chef Org","organizacion":"Cocina Org"}
                                                """
                                                        .formatted(email)))
                        .andExpect(status().isCreated())
                        .andReturn();

        String token =
                objectMapper
                        .readTree(registro.getResponse().getContentAsString())
                        .get("token")
                        .asString();

        mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.organizacion").value("Cocina Org"));

        String usuarios =
                mockMvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertThat(objectMapper.readTree(usuarios)).hasSize(1);
        assertThat(objectMapper.readTree(usuarios).get(0).get("email").asString())
                .isEqualTo(email);
    }
}
