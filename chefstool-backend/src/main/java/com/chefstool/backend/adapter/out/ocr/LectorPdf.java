package com.chefstool.backend.adapter.out.ocr;

import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

/** Extrae el texto embebido de un PDF digital (sin OCR). */
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
