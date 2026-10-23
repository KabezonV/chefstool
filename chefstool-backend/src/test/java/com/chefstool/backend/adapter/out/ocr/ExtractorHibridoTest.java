package com.chefstool.backend.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chefstool.backend.application.DocumentoIlegibleException;
import com.chefstool.backend.application.ParserAlbaran;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExtractorHibridoTest {

    private static final UUID ORG = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final ResultadoExtraccion local =
            new ResultadoExtraccion(
                    new ExtraccionDocumento(
                            "Local", null, null, null, "EUR",
                            BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ONE, List.of()),
                    new BigDecimal("0.20"));

    @Test
    void usaLaIaCuandoEstaDisponible() {
        ExtractorVision vision = mock(ExtractorVision.class);
        when(vision.disponible()).thenReturn(true);
        when(vision.extraer(any(), any(), any(), any())).thenReturn(local);

        ResultadoExtraccion resultado =
                new ExtractorHibrido(vision, textoLocal()).extraer(new byte[] {1}, "a.jpg", "image/jpeg", ORG);

        assertThat(resultado).isSameAs(local);
        verify(vision).extraer(any(), any(), any(), any());
    }

    @Test
    void siLaIaFallaUsaTesseract() {
        ExtractorVision vision = mock(ExtractorVision.class);
        when(vision.disponible()).thenReturn(true);
        when(vision.extraer(any(), any(), any(), any()))
                .thenThrow(new DocumentoIlegibleException("sin cuota"));

        ResultadoExtraccion resultado =
                new ExtractorHibrido(vision, textoLocal())
                        .extraer(new byte[] {1}, "a.jpg", "image/jpeg", ORG);

        assertThat(resultado.extraccion().proveedorNombre()).isEqualTo("Local");
    }

    @Test
    void sinClaveNoLlamaALaIa() {
        ExtractorVision vision = mock(ExtractorVision.class);
        when(vision.disponible()).thenReturn(false);

        new ExtractorHibrido(vision, textoLocal()).extraer(new byte[] {1}, "a.jpg", "image/jpeg", ORG);

        verify(vision, never()).extraer(any(), any(), any(), any());
    }

    private ExtractorTexto textoLocal() {
        ProductoRepository productos = mock(ProductoRepository.class);
        ProveedorRepository proveedores = mock(ProveedorRepository.class);
        ExtractorTexto real =
                new ExtractorTexto(
                        new LectorPdf(),
                        new LectorImagen("tesseract-no-instalado", "spa"),
                        new ParserAlbaran(productos, proveedores),
                        new ExtractorDemo(productos, proveedores));
        return new ExtractorTexto(
                new LectorPdf(),
                new LectorImagen("tesseract-no-instalado", "spa"),
                new ParserAlbaran(productos, proveedores),
                new ExtractorDemo(productos, proveedores)) {
            @Override
            public ResultadoExtraccion extraer(
                    byte[] contenido, String nombre, String tipo, UUID organizacionId) {
                return local;
            }
        };
    }
}
