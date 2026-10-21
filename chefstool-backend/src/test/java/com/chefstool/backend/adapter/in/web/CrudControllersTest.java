package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CrudControllersTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void cicloCompletoDeProveedor() throws Exception {
        String nombre = unico("Proveedor Norte");
        String id = crear("/api/proveedores", "{\"nombre\":\"%s\",\"cif\":\"B1234\"}".formatted(nombre));

        assertThat(nombres("/api/proveedores")).contains(nombre);

        String renombrado = unico("Proveedor Sur");
        mockMvc.perform(
                        put("/api/proveedores/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"%s\"}".formatted(renombrado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(renombrado));

        mockMvc.perform(delete("/api/proveedores/" + id)).andExpect(status().isNoContent());

        assertThat(nombres("/api/proveedores")).doesNotContain(renombrado);
        mockMvc.perform(delete("/api/proveedores/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void cicloCompletoDeCliente() throws Exception {
        String nombre = unico("Hotel Costa");
        String id =
                crear("/api/clientes", "{\"nombre\":\"%s\",\"email\":\"a@b.es\"}".formatted(nombre));

        String renombrado = unico("Hotel Costa 2");
        mockMvc.perform(
                        put("/api/clientes/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"%s\"}".formatted(renombrado)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value(renombrado));

        mockMvc.perform(delete("/api/clientes/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void cicloCompletoDeEventoConCliente() throws Exception {
        String cliente = unico("Boda Garcia");
        String clienteId =
                crear("/api/clientes", "{\"nombre\":\"%s\"}".formatted(cliente));

        String id =
                crear(
                        "/api/eventos",
                        """
                        {"nombre":"Banquete 120","fecha":"2026-11-20","comensales":120,
                         "clienteId":"%s"}
                        """
                                .formatted(clienteId));

        mockMvc.perform(get("/api/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].clienteNombre").value(cliente));

        mockMvc.perform(delete("/api/eventos/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void eventoConClienteInexistenteDevuelve404() throws Exception {
        mockMvc.perform(
                        post("/api/eventos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"nombre":"Sin cliente","clienteId":"%s"}
                                        """
                                                .formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaProveedorSinNombre() throws Exception {
        mockMvc.perform(
                        post("/api/proveedores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rechazaClienteDuplicado() throws Exception {
        String nombre = unico("Cliente Unico");
        crear("/api/clientes", "{\"nombre\":\"%s\"}".formatted(nombre));

        mockMvc.perform(
                        post("/api/clientes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"nombre\":\"%s\"}".formatted(nombre.toUpperCase())))
                .andExpect(status().isBadRequest());
    }

    private String unico(String base) {
        return base + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    private String crear(String ruta, String cuerpo) throws Exception {
        String respuesta =
                mockMvc.perform(
                                post(ruta)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asString();
    }

    private List<String> nombres(String ruta) throws Exception {
        String respuesta =
                mockMvc.perform(get(ruta))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        List<String> nombres = new ArrayList<>();
        for (var nodo : objectMapper.readTree(respuesta)) {
            nombres.add(nodo.get("nombre").asString());
        }
        return nombres;
    }
}
