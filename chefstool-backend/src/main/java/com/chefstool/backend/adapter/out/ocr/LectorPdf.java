package com.chefstool.backend.adapter.out.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Extrae el texto de un PDF: primero el embebido y, si es un escaneo, sus paginas como imagen. */
@Component
public class LectorPdf {

    public boolean esPdf(byte[] contenido) {
        if (contenido == null || contenido.length < 4) {
            return false;
        }
        return contenido[0] == '%'
                && contenido[1] == 'P'
                && contenido[2] == 'D'
                && contenido[3] == 'F';
    }

    /**
     * Convierte las primeras paginas del PDF en imagenes PNG para pasarlas por OCR.
     * Un escaneo no tiene texto embebido, asi que hay que "mirarlo" como una foto.
     */
    public java.util.List<byte[]> paginasComoPng(byte[] contenido, int maximoPaginas, int dpi) {
        java.util.List<byte[]> paginas = new java.util.ArrayList<>();
        try (PDDocument documento = Loader.loadPDF(contenido)) {
            PDFRenderer renderizador = new PDFRenderer(documento);
            int total = Math.min(documento.getNumberOfPages(), Math.max(maximoPaginas, 1));
            for (int pagina = 0; pagina < total; pagina++) {
                BufferedImage imagen = renderizador.renderImageWithDPI(pagina, dpi, ImageType.RGB);
                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ImageIO.write(imagen, "png", salida);
                paginas.add(salida.toByteArray());
            }
            return paginas;
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo convertir el PDF: " + ex.getMessage(), ex);
        }
    }

    public String extraerTexto(byte[] contenido) {
        try (PDDocument documento = Loader.loadPDF(contenido)) {
            PDFTextStripper extractor = new PDFTextStripper();
            extractor.setSortByPosition(true);
            return extractor.getText(documento);
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el PDF: " + ex.getMessage(), ex);
        }
    }
}
