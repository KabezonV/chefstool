package com.chefstool.backend.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.application.ParserAlbaran;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LectorPdfTest {

    private final LectorPdf lectorPdf = new LectorPdf();

    @Test
    void extraeElTextoDeUnPdfDigital() throws Exception {
        byte[] pdf = pdfConTexto("Frutas Garcia", "Albaran A-123");

        assertThat(lectorPdf.esPdf(pdf)).isTrue();
        assertThat(lectorPdf.extraerTexto(pdf)).contains("Frutas Garcia").contains("A-123");
    }

    @Test
    void convierteLasPaginasEnImagenesPng() throws Exception {
        byte[] pdf = pdfConTexto("pagina 1", "pagina 2");

        List<byte[]> paginas = lectorPdf.paginasComoPng(pdf, 3, 150);

        assertThat(paginas).hasSize(2);
        for (byte[] pagina : paginas) {
            assertThat(pagina[0] & 0xFF).isEqualTo(0x89);
            assertThat(new String(pagina, 1, 3)).isEqualTo("PNG");
        }
    }

    @Test
    void respetaElMaximoDePaginas() throws Exception {
        byte[] pdf = pdfConTexto("uno", "dos", "tres");

        assertThat(lectorPdf.paginasComoPng(pdf, 2, 72)).hasSize(2);
    }

    @Test
    void unPdfEscaneadoNoTieneTextoYSeRasteriza() throws Exception {
        byte[] pdf = pdfEscaneado();

        assertThat(lectorPdf.extraerTexto(pdf).isBlank()).isTrue();
        assertThat(lectorPdf.paginasComoPng(pdf, 1, 100)).isNotEmpty();
    }

    @Test
    void sinOcrDisponibleElPdfEscaneadoAvisaClaromente() throws Exception {
        ProductoRepository productos = Mockito.mock(ProductoRepository.class);
        ProveedorRepository proveedores = Mockito.mock(ProveedorRepository.class);
        LectorImagen sinOcr = new LectorImagen("tesseract-no-instalado-en-el-test", "spa");

        ExtractorTexto extractor =
                new ExtractorTexto(
                        lectorPdf,
                        sinOcr,
                        new ParserAlbaran(productos, proveedores),
                        new ExtractorDemo(productos, proveedores));

        assertThatThrownBy(
                        () ->
                                extractor.extraer(
                                        pdfEscaneado(),
                                        "escaneo.pdf",
                                        "application/pdf",
                                        UUID.randomUUID()))
                .isInstanceOf(DocumentoIlegibleException.class)
                .hasMessageContaining("escaneo");
    }

    private byte[] pdfConTexto(String... lineas) throws Exception {
        try (PDDocument documento = new PDDocument()) {
            for (String linea : lineas) {
                PDPage pagina = new PDPage();
                documento.addPage(pagina);
                try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                    contenido.beginText();
                    contenido.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 14);
                    contenido.newLineAtOffset(72, 700);
                    contenido.showText(linea);
                    contenido.endText();
                }
            }
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** PDF sin capa de texto: solo una imagen, como un escaneo. */
    private byte[] pdfEscaneado() throws Exception {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);

            BufferedImage imagen = new BufferedImage(600, 200, BufferedImage.TYPE_INT_RGB);
            PDImageXObject xObject = LosslessFactory.createFromImage(documento, imagen);

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.drawImage(xObject, 50, 500, 500, 160);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }
}
