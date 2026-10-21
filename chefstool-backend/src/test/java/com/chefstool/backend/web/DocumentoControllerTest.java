package com.chefstool.backend.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(
        properties = "chefstool.almacenamiento.directorio=${java.io.tmpdir}/chefstool-test-uploads")
class DocumentoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void subeUnArchivoYExtraeLasLineas() throws Exception {
        MockMultipartFile archivo =
                new MockMultipartFile(
                        "archivo",
                        "albaran-frutas.jpg",
                        "image/jpeg",
                        "contenido de prueba".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/documentos").file(archivo).param("origen", "subida"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.estado").value("pendiente_revision"))
                .andExpect(jsonPath("$.origen").value("subida"))
                .andExpect(jsonPath("$.tieneArchivo").value(true))
                .andExpect(jsonPath("$.confianzaExtraccion").exists())
                .andExpect(jsonPath("$.extraccion.lineas").isArray());
    }

    @Test
    void creaDocumentoDeDemostracionSinArchivo() throws Exception {
        mockMvc.perform(multipart("/api/documentos").param("origen", "demo"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origen").value("demo"))
                .andExpect(jsonPath("$.tieneArchivo").value(false))
                .andExpect(jsonPath("$.extraccion.numeroDocumento").exists());
    }

    @Test
    void guardaBorradorYConfirmaLaCompra() throws Exception {
        String id = subirDemo();

        String extraccion =
                """
                {"proveedorNombre":"Proveedor demo","numeroDocumento":"ALB-77",
                 "fecha":"2026-03-10","moneda":"EUR","importeNeto":30.00,
                 "importeIva":3.00,"importeTotal":33.00,
                 "lineas":[{"descripcion":"Tomate","cantidad":3,"unidad":"kg",
                            "precioUnitario":10.00,"importe":30.00}]}
                """;

        mockMvc.perform(
                        put("/api/documentos/" + id)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extraccion.numeroDocumento").value("ALB-77"));

        mockMvc.perform(
                        post("/api/documentos/" + id + "/confirmar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.numeroLineas").value(1))
                .andExpect(jsonPath("$.numeroDocumento").value("ALB-77"));

        mockMvc.perform(get("/api/documentos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("confirmado"));
    }

    @Test
    void noPermiteConfirmarDosVeces() throws Exception {
        String id = subirDemo();
        String extraccion = extraccionMinima();

        mockMvc.perform(
                        post("/api/documentos/" + id + "/confirmar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccion))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/documentos/" + id + "/confirmar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccion))
                .andExpect(status().isConflict());
    }

    @Test
    void descartaElDocumentoYLoLista() throws Exception {
        String id = subirDemo();

        mockMvc.perform(delete("/api/documentos/" + id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].estado").value("descartado"));
    }

    @Test
    void documentoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/documentos/00000000-0000-0000-0000-0000000000ff"))
                .andExpect(status().isNotFound());
    }

    private String subirDemo() throws Exception {
        String respuesta =
                mockMvc.perform(multipart("/api/documentos").param("origen", "demo"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        return objectMapper.readTree(respuesta).get("id").asString();
    }

    private String extraccionMinima() {
        return """
               {"proveedorNombre":"Proveedor demo","numeroDocumento":"ALB-1",
                "fecha":"2026-04-01","moneda":"EUR","importeNeto":10.00,
                "importeIva":1.00,"importeTotal":11.00,
                "lineas":[{"descripcion":"Lechuga","cantidad":1,"unidad":"kg",
                           "precioUnitario":10.00,"importe":10.00}]}
               """;
    }
}
