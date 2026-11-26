package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
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
class AuthControllerTest extends IntegracionBase {

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
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.localId").isNotEmpty())
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
    void refrescaYRevocaLaSesion() throws Exception {
        String email = "refresh-" + UUID.randomUUID() + "@chefstool.es";
        String registro = mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"%s\",\"password\":\"secreto123\"}"
                                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(registro).get("refreshToken").asText();

        mockMvc.perform(
                        post("/api/auth/refrescar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/auth/refrescar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
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

    @Test
    void unaPersonaAutenticadaPuedeSolicitarPeroNoAprobarSuPropiaCompra() throws Exception {
        String email = "solicitud-" + UUID.randomUUID() + "@chefstool.es";
        MvcResult registro =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"email":"%s","password":"secreto123",
                                                 "nombre":"Chef Solicitante","organizacion":"Cocina Solicitudes"}
                                                """
                                                        .formatted(email)))
                        .andExpect(status().isCreated())
                        .andReturn();
        String token =
                objectMapper
                        .readTree(registro.getResponse().getContentAsString())
                        .get("token")
                        .asString();

        MvcResult solicitud =
                mockMvc.perform(
                                post("/api/solicitudes-compra")
                                        .header("Authorization", "Bearer " + token)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"concepto":"Horno","importe":1200.00,
                                                 "proveedor":"Proveedor de prueba"}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                        .andExpect(jsonPath("$.solicitanteNombre").value("Chef Solicitante"))
                        .andReturn();
        String solicitudId =
                objectMapper
                        .readTree(solicitud.getResponse().getContentAsString())
                        .get("id")
                        .asString();

        mockMvc.perform(
                        post("/api/solicitudes-compra/{id}/aprobar", solicitudId)
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorApruebaSolicitudDeUnMiembroYEsteNoPuedeDecidirla() throws Exception {
        String propietarioEmail = "jefa-" + UUID.randomUUID() + "@chefstool.es";
        String miembroEmail = "miembro-" + UUID.randomUUID() + "@chefstool.es";
        String propietarioToken =
                registrarYObtenerToken(
                        propietarioEmail, "Jefa Cocina", "Cocina Aprobaciones");

        mockMvc.perform(
                        post("/api/usuarios/invitar")
                                .header("Authorization", "Bearer " + propietarioToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"email":"%s","rol":"miembro"}
                                        """
                                                .formatted(miembroEmail)))
                .andExpect(status().isCreated());

        String miembroToken =
                registrarYObtenerToken(miembroEmail, "Miembro Cocina", null);
        MvcResult solicitud =
                mockMvc.perform(
                                post("/api/solicitudes-compra")
                                        .header("Authorization", "Bearer " + miembroToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"concepto":"Batidora industrial","importe":350.00}
                                                """))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                        .andReturn();
        String solicitudId =
                objectMapper
                        .readTree(solicitud.getResponse().getContentAsString())
                        .get("id")
                        .asString();

        mockMvc.perform(
                        get("/api/solicitudes-compra")
                                .header("Authorization", "Bearer " + miembroToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(solicitudId));

        mockMvc.perform(
                        post("/api/solicitudes-compra/{id}/aprobar", solicitudId)
                                .header("Authorization", "Bearer " + miembroToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        post("/api/solicitudes-compra/{id}/aprobar", solicitudId)
                                .header("Authorization", "Bearer " + propietarioToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("APROBADA"))
                .andExpect(jsonPath("$.revisadaPorNombre").value("Jefa Cocina"));

        MvcResult segundaSolicitud =
                mockMvc.perform(
                                post("/api/solicitudes-compra")
                                        .header("Authorization", "Bearer " + miembroToken)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                                {"concepto":"Horno adicional","importe":2000.00}
                                                """))
                        .andExpect(status().isCreated())
                        .andReturn();
        String segundaSolicitudId =
                objectMapper
                        .readTree(segundaSolicitud.getResponse().getContentAsString())
                        .get("id")
                        .asString();

        mockMvc.perform(
                        post("/api/solicitudes-compra/{id}/rechazar", segundaSolicitudId)
                                .header("Authorization", "Bearer " + propietarioToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"motivo\":\"No hay presupuesto aprobado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("No hay presupuesto aprobado"));
    }

    private String registrarYObtenerToken(String email, String nombre, String organizacion)
            throws Exception {
        String cuerpo =
                organizacion == null
                        ? """
                          {"email":"%s","password":"secreto123","nombre":"%s"}
                          """
                                .formatted(email, nombre)
                        : """
                          {"email":"%s","password":"secreto123","nombre":"%s",
                           "organizacion":"%s"}
                          """
                                .formatted(email, nombre, organizacion);
        MvcResult registro =
                mockMvc.perform(
                                post("/api/auth/registro")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo))
                        .andExpect(status().isCreated())
                        .andReturn();
        return objectMapper
                .readTree(registro.getResponse().getContentAsString())
                .get("token")
                .asString();
    }

    @Test
    void ignoraLosEspaciosQueAnadeElTeclado() throws Exception {
        String email = "espacios-" + UUID.randomUUID() + "@chefstool.es";

        mockMvc.perform(
                        post("/api/auth/registro")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"%s\",\"password\":\"secreto123\"}"
                                        .formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\" %s \",\"password\":\"secreto123 \"}"
                                        .formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
