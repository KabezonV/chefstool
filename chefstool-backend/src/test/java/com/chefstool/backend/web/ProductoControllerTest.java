package com.chefstool.backend.web;

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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductoControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void listaProductos() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void creaProducto() throws Exception {
        String nombre = "Producto " + UUID.randomUUID();
        String cuerpo =
                """
                {"nombre":"%s","categoria":"Secos","unidadBase":"kg"}
                """
                        .formatted(nombre);

        mockMvc.perform(
                        post("/api/productos")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value(nombre))
                .andExpect(jsonPath("$.unidadBase").value("kg"));
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
}
