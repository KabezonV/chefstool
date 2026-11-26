package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.IntegracionBase;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
class DocumentoControllerTest extends IntegracionBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ProveedorRepository proveedorRepository;

    @Test
    void guardaLaImagenAunqueNoSePuedaLeer() throws Exception {
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
                .andExpect(jsonPath("$.tieneArchivo").value(true));
    }

    @Test
    void extraeLineasDeUnPdfConTexto() throws Exception {
        String sufijo = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String nombreProveedor = "Proveedor test " + sufijo;
        String nombreProducto = "Tomate test " + sufijo;

        Proveedor proveedor = new Proveedor();
        proveedor.setOrganizacionId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        proveedor.setNombre(nombreProveedor);
        proveedorRepository.save(proveedor);

        Producto producto = new Producto();
        producto.setOrganizacionId(proveedor.getOrganizacionId());
        producto.setNombre(nombreProducto);
        producto.setUnidadBase("kg");
        producto = productoRepository.save(producto);

        byte[] pdf =
                pdf(
                        nombreProveedor,
                        "ALBARAN: ALB-" + sufijo,
                        "Fecha: 05/10/2026",
                        nombreProducto.toUpperCase() + " 10 kg 2,50 25,00",
                        "Base imponible 25,00",
                        "IVA 10% 2,50",
                        "TOTAL 27,50");

        String respuesta =
                mockMvc.perform(
                                multipart("/api/documentos")
                                        .file(
                                                new MockMultipartFile(
                                                        "archivo",
                                                        "albaran.pdf",
                                                        "application/pdf",
                                                        pdf))
                                        .param("origen", "subida"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        var detalle = objectMapper.readTree(respuesta);
        var extraccion = detalle.get("extraccion");

        assertThat(detalle.get("errorExtraccion").isNull()).isTrue();
        assertThat(detalle.get("confianzaExtraccion").asDouble()).isGreaterThan(0.7);
        assertThat(extraccion.get("proveedorNombre").asString()).isEqualTo(nombreProveedor);
        assertThat(extraccion.get("numeroDocumento").asString()).isEqualTo("ALB-" + sufijo);
        assertThat(extraccion.get("fecha").asString()).isEqualTo("2026-10-05");
        assertThat(extraccion.get("importeTotal").asDouble()).isEqualTo(27.50);
        assertThat(extraccion.get("lineas")).hasSize(1);
        assertThat(extraccion.get("lineas").get(0).get("productoId").asString())
                .isEqualTo(producto.getId().toString());
        assertThat(extraccion.get("lineas").get(0).get("cantidad").asDouble()).isEqualTo(10.0);
        assertThat(extraccion.get("lineas").get(0).get("unidad").asString()).isEqualTo("kg");
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
                .andExpect(jsonPath("$.contenido[?(@.id=='" + id + "')].estado").value("descartado"));
    }

    @Test
    void documentoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/api/documentos/00000000-0000-0000-0000-0000000000ff"))
                .andExpect(status().isNotFound());
    }

    @Test
    void anexaLaSegundaHojaYVuelveALeerElAlbaranCompleto() throws Exception {
        byte[] primeraHoja =
                pdf("DISTRIBUCIONES EJEMPLO S.L.", "ALBARAN: ALB-DOSHOJAS", "LECHUGA 2 kg 2,50 5,00");
        String subida =
                mockMvc.perform(
                                multipart("/api/documentos")
                                        .file(
                                                new MockMultipartFile(
                                                        "archivo", "hoja1.pdf", "application/pdf",
                                                        primeraHoja))
                                        .param("origen", "camara"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = objectMapper.readTree(subida).get("id").asString();

        byte[] segundaHoja =
                pdf("PAGINA 2", "Base imponible 37,80", "IVA 10% 3,78", "TOTAL 41,58");

        mockMvc.perform(
                        multipart("/api/documentos/" + id + "/hojas")
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "hoja2.pdf", "application/pdf",
                                                segundaHoja)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreOriginal").value(org.hamcrest.Matchers.endsWith("-2-hojas.pdf")))
                .andExpect(jsonPath("$.extraccion.importeTotal").value(41.58));

        // El archivo guardado tiene de verdad las dos paginas, no solo la ultima
        byte[] guardado =
                mockMvc.perform(get("/api/documentos/" + id + "/archivo"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();
        try (PDDocument documento = org.apache.pdfbox.Loader.loadPDF(guardado)) {
            assertThat(documento.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    void noAnexaHojasAUnDocumentoYaConfirmado() throws Exception {
        String id = subirDemo();
        mockMvc.perform(
                        post("/api/documentos/" + id + "/confirmar")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(extraccionMinima()))
                .andExpect(status().isOk());

        mockMvc.perform(
                        multipart("/api/documentos/" + id + "/hojas")
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "hoja2.pdf", "application/pdf",
                                                pdf("TOTAL 10,00"))))
                .andExpect(status().isConflict());
    }

    @Test
    void explicaPorQueNoSePuedeMezclarUnaFotoConUnPdf() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB), "png", png);
        String subida =
                mockMvc.perform(
                                multipart("/api/documentos")
                                        .file(
                                                new MockMultipartFile(
                                                        "archivo", "hoja1.png", "image/png",
                                                        png.toByteArray()))
                                        .param("origen", "camara"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = objectMapper.readTree(subida).get("id").asString();

        mockMvc.perform(
                        multipart("/api/documentos/" + id + "/hojas")
                                .file(
                                        new MockMultipartFile(
                                                "archivo", "hoja2.pdf", "application/pdf",
                                                pdf("TOTAL 10,00"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("fotos y PDFs")));
    }

    @Test
    void descargaElOriginalConNombreClaroEnLugarDeAbrirlo() throws Exception {
        byte[] hoja = pdf("ALBARAN: ALB-DESCARGA", "TOTAL 10,00");
        String subida =
                mockMvc.perform(
                                multipart("/api/documentos")
                                        .file(
                                                new MockMultipartFile(
                                                        "archivo", "albarán de prueba.pdf",
                                                        "application/pdf", hoja))
                                        .param("origen", "subida"))
                        .andExpect(status().isCreated())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String id = objectMapper.readTree(subida).get("id").asString();

        mockMvc.perform(get("/api/documentos/" + id + "/descarga"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")))
                .andExpect(header().string(
                        "Content-Disposition", org.hamcrest.Matchers.containsString(".pdf")));
    }

    @Test
    void empaquetaEnUnZipLosAlbaranesDelPeriodo() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(
                            multipart("/api/documentos")
                                    .file(
                                            new MockMultipartFile(
                                                    "archivo", "hoja-" + i + ".pdf",
                                                    "application/pdf",
                                                    pdf("ALBARAN: ALB-" + i, "TOTAL 10,00")))
                                    .param("origen", "subida"))
                    .andExpect(status().isCreated());
        }

        String hoy = java.time.LocalDate.now().toString();
        byte[] zip =
                mockMvc.perform(get("/api/documentos/zip").param("desde", hoy).param("hasta", hoy))
                        .andExpect(status().isOk())
                        .andExpect(header().string(
                                "Content-Disposition",
                                org.hamcrest.Matchers.containsString("albaranes-" + hoy)))
                        .andReturn()
                        .getResponse()
                        .getContentAsByteArray();

        java.util.List<String> entradas = new java.util.ArrayList<>();
        try (java.util.zip.ZipInputStream lector =
                new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            java.util.zip.ZipEntry entrada;
            while ((entrada = lector.getNextEntry()) != null) {
                entradas.add(entrada.getName());
            }
        }
        assertThat(entradas).hasSize(2);
        assertThat(entradas).allMatch(nombre -> nombre.endsWith(".pdf"));
    }

    @Test
    void avisaSiElPeriodoNoTieneAlbaranes() throws Exception {
        mockMvc.perform(
                        get("/api/documentos/zip")
                                .param("desde", "2000-01-01")
                                .param("hasta", "2000-01-31"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noEmpaquetaSinIndicarElPeriodo() throws Exception {
        mockMvc.perform(get("/api/documentos/zip")).andExpect(status().isBadRequest());
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

    @Test
    void juntaVariasFotosEnUnSoloDocumento() throws Exception {
        MockMultipartFile hoja1 = png("hoja1.png");
        MockMultipartFile hoja2 = png("hoja2.png");

        mockMvc.perform(
                        multipart("/api/documentos")
                                .file(hoja1)
                                .file(hoja2)
                                .param("origen", "subida"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombreOriginal").value("hoja1-2-hojas.pdf"))
                .andExpect(jsonPath("$.tieneArchivo").value(true));
    }

    private MockMultipartFile png(String nombre) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(40, 60, BufferedImage.TYPE_INT_RGB), "png", salida);
        return new MockMultipartFile("archivo", nombre, "image/png", salida.toByteArray());
    }
}
