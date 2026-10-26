package com.chefstool.backend.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import com.chefstool.backend.adapter.in.web.dto.FichaLineaResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class FichaPdfServiceTest {

    @Test
    void generaUnPdfConLosDatosDeLaFicha() throws Exception {
        UUID id = UUID.randomUUID();
        FichaService fichas = mock(FichaService.class);
        when(fichas.obtener(id))
                .thenReturn(
                        new FichaDetalle(
                                id,
                                "Paella de marisco",
                                "Receta de la casa",
                                10,
                                null,
                                new BigDecimal("18.50"),
                                new BigDecimal("16.30"),
                                new BigDecimal("1.63"),
                                new BigDecimal("86.4"),
                                0,
                                List.of(
                                        new FichaLineaResponse(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                "Arroz bomba",
                                                "Arroz bomba",
                                                new BigDecimal("1.2"),
                                                "kg",
                                                new BigDecimal("2.10"),
                                                new BigDecimal("2.52")),
                                        new FichaLineaResponse(
                                                UUID.randomUUID(),
                                                UUID.randomUUID(),
                                                "Gamba",
                                                "Gamba pelada",
                                                new BigDecimal("0.6"),
                                                "kg",
                                                new BigDecimal("12.90"),
                                                new BigDecimal("7.74")))));

        byte[] pdf = new FichaPdfService(fichas).generar(id, "Cocina Norte");

        assertThat(pdf).startsWith("%PDF".getBytes());
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento);
            assertThat(texto).contains("Ficha tecnica");
            assertThat(texto).contains("Paella de marisco");
            assertThat(texto).contains("Cocina Norte");
            assertThat(texto).contains("Arroz bomba");
            assertThat(texto).contains("Gamba pelada");
            assertThat(texto).contains("Margen");
            assertThat(texto).contains("Raciones: 10");
        }
    }
}
