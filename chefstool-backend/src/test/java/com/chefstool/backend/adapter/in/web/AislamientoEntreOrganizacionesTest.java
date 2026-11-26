package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.IntegracionBase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Una organización no puede ver, tocar ni borrar nada de otra aunque conozca el identificador.
 * Es la frontera de seguridad del producto: si esto falla, un cliente ve datos de otro.
 */
class AislamientoEntreOrganizacionesTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void unProductoDeOtraOrganizacionNoExiste() throws Exception {
        JsonNode jefeA = registro("a-" + UUID.randomUUID() + "@cocina.es");
        JsonNode jefeB = registro("b-" + UUID.randomUUID() + "@cocina.es");

        String producto = crear(jefeA, "/api/productos", "{\"nombre\":\"Privado\",\"unidadBase\":\"kg\"}");

        mockMvc.perform(get("/api/productos/" + producto + "/precios")
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/productos/" + producto)
                        .header("Authorization", token(jefeB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Secuestrado\",\"unidadBase\":\"kg\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/productos/" + producto)
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());

        // Y sigue existiendo para su dueño
        mockMvc.perform(get("/api/productos/" + producto + "/precios")
                        .header("Authorization", token(jefeA)))
                .andExpect(status().isOk());
    }

    @Test
    void unProveedorDeOtraOrganizacionNoSePuedeTocarNiBorrar() throws Exception {
        JsonNode jefeA = registro("a-" + UUID.randomUUID() + "@cocina.es");
        JsonNode jefeB = registro("b-" + UUID.randomUUID() + "@cocina.es");

        String proveedor = crear(jefeA, "/api/proveedores", "{\"nombre\":\"Proveedor A\"}");

        mockMvc.perform(put("/api/proveedores/" + proveedor)
                        .header("Authorization", token(jefeB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Cambiado por B\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/proveedores/" + proveedor)
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unAlbaranDeOtraOrganizacionNoSePuedeVerNiDescargar() throws Exception {
        JsonNode jefeA = registro("a-" + UUID.randomUUID() + "@cocina.es");
        JsonNode jefeB = registro("b-" + UUID.randomUUID() + "@cocina.es");

        String documento = subirAlbaran(jefeA);

        mockMvc.perform(get("/api/documentos/" + documento).header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/documentos/" + documento + "/archivo")
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/documentos/" + documento + "/descarga")
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/documentos/" + documento)
                        .header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unaCompraDeOtraOrganizacionNoSePuedeAbrirNiDescargar() throws Exception {
        JsonNode jefeA = registro("a-" + UUID.randomUUID() + "@cocina.es");
        JsonNode jefeB = registro("b-" + UUID.randomUUID() + "@cocina.es");

        String documento = subirAlbaran(jefeA);
        String extraccion = extraccion();
        String compra = objectMapper.readTree(
                        mockMvc.perform(post("/api/documentos/" + documento + "/confirmar")
                                        .header("Authorization", token(jefeA))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(extraccion))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .get("id")
                .asString();

        mockMvc.perform(get("/api/compras/" + compra).header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/compras/" + compra + "/pdf").header("Authorization", token(jefeB)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/compras/" + compra).header("Authorization", token(jefeA)))
                .andExpect(status().isOk());
    }

    @Test
    void elZipDeUnPeriodoNoMezclaAlbaranesDeOtrasOrganizaciones() throws Exception {
        JsonNode jefeA = registro("a-" + UUID.randomUUID() + "@cocina.es");
        JsonNode jefeB = registro("b-" + UUID.randomUUID() + "@cocina.es");
        subirAlbaran(jefeA);

        String hoy = java.time.LocalDate.now().toString();
        byte[] zipDeB = mockMvc.perform(get("/api/documentos/zip")
                        .param("desde", hoy)
                        .param("hasta", hoy)
                        .header("Authorization", token(jefeB)))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(new String(zipDeB)).doesNotContain("ALBARAN");
    }

    private String subirAlbaran(JsonNode jefe) throws Exception {
        return objectMapper.readTree(
                        mockMvc.perform(multipart("/api/documentos")
                                        .file(new MockMultipartFile(
                                                "archivo", "albaran.pdf", "application/pdf", pdf()))
                                        .param("origen", "subida")
                                        .header("Authorization", token(jefe)))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .get("id")
                .asString();
    }

    private String crear(JsonNode jefe, String ruta, String cuerpo) throws Exception {
        return objectMapper.readTree(
                        mockMvc.perform(post(ruta)
                                        .header("Authorization", token(jefe))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(cuerpo))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString())
                .get("id")
                .asString();
    }

    private String extraccion() {
        return """
               {"proveedorNombre":"Proveedor A","numeroDocumento":"ALB-AISLAMIENTO",
                "fecha":"2026-11-20","moneda":"EUR","importeNeto":10.00,
                "importeIva":1.00,"importeTotal":11.00,
                "lineas":[{"descripcion":"Lechuga","cantidad":1,"unidad":"kg",
                           "precioUnitario":10.00,"importe":10.00}]}
               """;
    }

    private byte[] pdf() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.beginText();
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contenido.newLineAtOffset(50, 780);
                contenido.showText("ALBARAN: ALB-AISLAMIENTO");
                contenido.endText();
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    private String token(JsonNode sesion) {
        return "Bearer " + sesion.get("token").asString();
    }

    private JsonNode registro(String email) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123","nombre":"Jefe",
                                 "organizacion":"Cocina %s"}
                                """.formatted(email, UUID.randomUUID().toString().substring(0, 8))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(respuesta);
    }
}
