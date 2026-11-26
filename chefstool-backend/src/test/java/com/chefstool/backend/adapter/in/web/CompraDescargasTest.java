package com.chefstool.backend.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.IntegracionBase;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/** Descargas de compras: resumen en PDF y ZIP de los albaranes originales. */
@ActiveProfiles("test")
class CompraDescargasTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void generaElResumenDeLaCompraEnPdf() throws Exception {
        String compraId = compraConfirmada("A-100", "Proveedor Uno", "10.00");

        byte[] pdf =
                mockMvc.perform(get("/api/compras/" + compraId + "/pdf"))
                        .andExpect(status().isOk())
                        .andExpect(header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.startsWith("attachment")))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
        try (PDDocument documento = org.apache.pdfbox.Loader.loadPDF(pdf)) {
            assertThat(documento.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void empaquetaLosAlbaranesDeLasComprasDelPeriodo() throws Exception {
        compraConfirmada("A-200", "Proveedor Uno", "10.00");
        compraConfirmada("A-201", "Proveedor Dos", "20.00");

        String hoy = java.time.LocalDate.now().toString();
        byte[] zip = zip(null, hoy, hoy);

        List<String> entradas = entradas(zip);
        assertThat(entradas).hasSize(2);
        assertThat(entradas).anyMatch(nombre -> nombre.contains("A-200"));
        assertThat(entradas).anyMatch(nombre -> nombre.contains("A-201"));
    }

    @Test
    void elZipSePuedeLimitarAUnProveedor() throws Exception {
        compraConfirmada("A-300", "Proveedor Uno", "10.00");
        String compraDos = compraConfirmada("A-301", "Proveedor Dos", "20.00");
        String proveedorDos = proveedorDe(compraDos);

        String hoy = java.time.LocalDate.now().toString();
        List<String> entradas = entradas(zip(proveedorDos, hoy, hoy));

        assertThat(entradas).hasSize(1);
        assertThat(entradas.getFirst()).contains("A-301");
    }

    @Test
    void avisaSiElPeriodoNoTieneCompras() throws Exception {
        mockMvc.perform(
                        get("/api/compras/zip")
                                .param("desde", "2000-01-01")
                                .param("hasta", "2000-01-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEmpaquetaSinPeriodo() throws Exception {
        mockMvc.perform(get("/api/compras/zip")).andExpect(status().isBadRequest());
    }

    private byte[] zip(String proveedorId, String desde, String hasta) throws Exception {
        var peticion = get("/api/compras/zip").param("desde", desde).param("hasta", hasta);
        if (proveedorId != null) {
            peticion.param("proveedorId", proveedorId);
        }
        return mockMvc.perform(peticion)
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.startsWith("attachment")))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
    }

    private String proveedorDe(String compraId) throws Exception {
        String respuesta = mockMvc.perform(get("/api/compras/" + compraId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(respuesta).get("proveedorId").asString();
    }

    private List<String> entradas(byte[] zip) throws IOException {
        List<String> nombres = new ArrayList<>();
        try (ZipInputStream lector = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entrada;
            while ((entrada = lector.getNextEntry()) != null) {
                nombres.add(entrada.getName());
            }
        }
        return nombres;
    }

    /** Sube un albaran con archivo real y lo confirma, devolviendo el id de la compra. */
    private String compraConfirmada(String numero, String proveedor, String importe)
            throws Exception {
        String subida = mockMvc.perform(
                        multipart("/api/documentos")
                                .file(new MockMultipartFile(
                                        "archivo", "albaran-" + numero + ".pdf",
                                        "application/pdf",
                                        pdf("ALBARAN: " + numero)))
                                .param("origen", "subida"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String documentoId = objectMapper.readTree(subida).get("id").asString();

        String extraccion = """
                {"proveedorNombre":"%s","numeroDocumento":"%s",
                 "fecha":"%s","moneda":"EUR","importeNeto":%s,
                 "importeIva":1.00,"importeTotal":%s,
                 "lineas":[{"descripcion":"Lechuga","cantidad":1,"unidad":"kg",
                            "precioUnitario":%s,"importe":%s}]}
                """
                .formatted(
                        proveedor,
                        numero,
                        java.time.LocalDate.now(),
                        importe,
                        new java.math.BigDecimal(importe).add(new java.math.BigDecimal("1.00")),
                        importe,
                        importe);

        String compra = mockMvc.perform(
                        post("/api/documentos/" + documentoId + "/confirmar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccion))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(compra).get("id").asString();
    }

    private byte[] pdf(String... lineas) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.beginText();
                contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contenido.newLineAtOffset(50, 780);
                for (String linea : lineas) {
                    contenido.showText(linea);
                    contenido.newLineAtOffset(0, -16);
                }
                contenido.endText();
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }
}
