package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** Corre con la seguridad ACTIVA: comprueba invitaciones y permisos por rol. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EquipoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void invitaPorCorreoYAceptaAlRegistrarse() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Cocina Jefe");

        String emailInvitado = "chef-" + UUID.randomUUID() + "@cocina.es";

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", "Bearer " + propietario.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","rol":"miembro"}
                                        """
                                                .formatted(emailInvitado)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("pendiente"))
                .andExpect(jsonPath("$.email").value(emailInvitado));

        // El invitado se registra y entra en la MISMA organizacion
        JsonNode invitado = registroConOrganizacion(emailInvitado, "Cocina Propia");

        assertThat(invitado.get("organizacionId").asString())
                .isEqualTo(propietario.get("organizacionId").asString());
        assertThat(invitado.get("organizacion").asString())
                .isEqualTo(propietario.get("organizacion").asString());
        assertThat(invitado.get("rol").asString()).isEqualTo("miembro");

        mockMvc.perform(
                        get("/api/usuarios")
                                .header("Authorization", "Bearer " + propietario.get("token").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void anadeDirectamenteAUnUsuarioQueYaExiste() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Cocina A");
        String emailExistente = "externo-" + UUID.randomUUID() + "@cocina.es";
        registro(emailExistente, "Cocina B");

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", "Bearer " + propietario.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","rol":"admin"}
                                        """
                                                .formatted(emailExistente)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("anadido"))
                .andExpect(jsonPath("$.rol").value("admin"));

        mockMvc.perform(
                        get("/api/usuarios")
                                .header("Authorization", "Bearer " + propietario.get("token").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void unMiembroNoPuedeInvitarNiCambiarRoles() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Cocina C");
        String emailMiembro = "miembro-" + UUID.randomUUID() + "@cocina.es";

        invitar(propietario, emailMiembro, "miembro");
        JsonNode miembro = registroConOrganizacion(emailMiembro, "Ignorada");

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", "Bearer " + miembro.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"otro@cocina.es\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        put("/api/organizacion")
                                .header("Authorization", "Bearer " + miembro.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"Nuevo nombre\"}"))
                .andExpect(status().isForbidden());

        // Pero si puede trabajar con los datos
        mockMvc.perform(
                        get("/api/productos")
                                .header("Authorization", "Bearer " + miembro.get("token").asString()))
                .andExpect(status().isOk());
    }

    @Test
    void unAdminPuedeInvitarYRenombrarLaOrganizacion() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Cocina D");
        String emailAdmin = "admin-" + UUID.randomUUID() + "@cocina.es";

        invitar(propietario, emailAdmin, "admin");
        JsonNode admin = registroConOrganizacion(emailAdmin, "Ignorada");

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", "Bearer " + admin.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"nuevo@cocina.es\",\"rol\":\"miembro\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        put("/api/organizacion")
                                .header("Authorization", "Bearer " + admin.get("token").asString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"Cocina Renombrada\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Cocina Renombrada"));
    }

    @Test
    void sePuedeCancelarUnaInvitacionPendiente() throws Exception {
        JsonNode propietario = registro("jefe-" + UUID.randomUUID() + "@cocina.es", "Cocina E");
        String email = "pendiente-" + UUID.randomUUID() + "@cocina.es";

        String id =
                invitar(propietario, email, "miembro")
                        .get("id")
                        .asString();

        mockMvc.perform(
                        get("/api/usuarios/invitaciones")
                                .header("Authorization", "Bearer " + propietario.get("token").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(
                        delete("/api/usuarios/invitaciones/" + id)
                                .header("Authorization", "Bearer " + propietario.get("token").asString()))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/usuarios/invitaciones")
                                .header("Authorization", "Bearer " + propietario.get("token").asString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private JsonNode invitar(JsonNode propietario, String email, String rol) throws Exception {
        String respuesta =
                mockMvc.perform(
                                post("/api/usuarios/invitar")
                                        .header(
                                                "Authorization",
                                                "Bearer " + propietario.get("token").asString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","rol":"%s"}
                                                """
                                                        .formatted(email, rol)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta);
    }

    private JsonNode registro(String email, String organizacion) throws Exception {
        return registroConOrganizacion(email, organizacion);
    }

    private JsonNode registroConOrganizacion(String email, String organizacion) throws Exception {
        String respuesta =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"secreto123",
                                                 "nombre":"Chef","organizacion":"%s"}
                                                """
                                                        .formatted(email, organizacion)))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta);
    }
}
