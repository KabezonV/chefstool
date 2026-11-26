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
import com.chefstool.backend.domain.model.LineaExtraccion;
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

    @Test
    void contrastaLineasDeIaConTesseractYSeQuedaConLaLecturaQueCuadra() {
        LineaExtraccion visionLinea =
                new LineaExtraccion(
                        "Cebolla blanca", new BigDecimal("2.40"), "kg",
                        new BigDecimal("1.90"), new BigDecimal("7.20"), null, null, null);
        LineaExtraccion localLinea =
                new LineaExtraccion(
                        "CEBOLLA BLANCA", new BigDecimal("3"), "kg",
                        new BigDecimal("1.20"), new BigDecimal("3.60"), null, null, null);

        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Proveedor IA", null, "A-2020/0457", null, "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("41.58"), List.of(visionLinea)),
                        new BigDecimal("0.98"));
        ResultadoExtraccion local =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO S.L.", null,
                                "A-2026/0457", java.time.LocalDate.of(2026, 11, 8), "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("41.58"), List.of(localLinea)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal())
                        .combinar(vision, local);

        assertThat(combinado.extraccion().numeroDocumento()).isEqualTo("A-2026/0457");
        assertThat(combinado.extraccion().fecha()).isEqualTo(java.time.LocalDate.of(2026, 11, 8));
        assertThat(combinado.extraccion().lineas()).hasSize(1);
        // La misma linea leida por los dos: antes ganaba siempre Tesseract. En fotos con sombra
        // devuelve cantidades inventadas, asi que ahora gana la lectura que cuadra mejor.
        assertThat(combinado.extraccion().lineas().getFirst().cantidad())
                .isEqualByComparingTo("2.40");
        assertThat(combinado.extraccion().lineas().getFirst().precioUnitario())
                .isEqualByComparingTo("1.90");
    }

    @Test
    void noDuplicaLaLineaQueUnLectorLeyoMal() {
        // Tesseract lee la misma linea con el texto roto y un importe identico
        LineaExtraccion visionLechuga = linea("LECHUGA ROMANA", "5.00");
        LineaExtraccion visionZanahoria = linea("ZANAHORIA", "5.50");
        LineaExtraccion localLechuga = linea("LE CHUGA ROMANA", "5.00");
        LineaExtraccion localZanahoria = linea("<ANAHOR1a", "5.50");

        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, null, null, "EUR",
                                new BigDecimal("10.50"), new BigDecimal("1.05"),
                                new BigDecimal("11.55"),
                                List.of(visionLechuga, visionZanahoria)),
                        new BigDecimal("0.98"));
        ResultadoExtraccion tesseract =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("10.50"), new BigDecimal("1.05"),
                                new BigDecimal("11.55"),
                                List.of(localLechuga, localZanahoria)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal()).combinar(vision, tesseract);

        assertThat(combinado.extraccion().lineas()).hasSize(2);
        assertThat(combinado.extraccion().lineas())
                .extracting(LineaExtraccion::descripcion)
                .containsExactly("LECHUGA ROMANA", "ZANAHORIA");
    }

    @Test
    void noJuntaDosProductosDistintosQueSoloCompartenUnaPalabra() {
        LineaExtraccion tomatePera = linea("TOMATE PERA", "7.40");
        LineaExtraccion tomateRama = linea("TOMATE RAMA", "7.40");

        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, null, null, "EUR",
                                new BigDecimal("14.80"), new BigDecimal("1.48"),
                                new BigDecimal("16.28"), List.of(tomatePera)),
                        new BigDecimal("0.90"));
        ResultadoExtraccion tesseract =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("14.80"), new BigDecimal("1.48"),
                                new BigDecimal("16.28"), List.of(tomateRama)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal()).combinar(vision, tesseract);

        assertThat(combinado.extraccion().lineas()).hasSize(2);
    }

    @Test
    void usaLaLecturaQueCuadraYLaMarcaParaRevisarSiLosLectoresDiscrepan() {
        // La IA lee bien un albaran con sombra; Tesseract se equivoca en la base y el total
        LineaExtraccion lineaVision = linea("Lechuga romana", "37.80");
        LineaExtraccion lineaTesseract = linea("LECHUGA ROMANA", "37.80");
        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, "A-2026/0457", null, "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("41.58"), List.of(lineaVision)),
                        new BigDecimal("0.98"));
        ResultadoExtraccion tesseract =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("40.89"), new BigDecimal("4.09"),
                                new BigDecimal("44.98"), List.of(lineaTesseract)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal()).combinar(vision, tesseract);

        // Se queda con el total que cuadra con sus lineas...
        assertThat(combinado.extraccion().importeTotal()).isEqualByComparingTo("41.58");
        // ...pero como los dos lectores no coinciden, no se presume de confianza alta
        assertThat(combinado.confianza()).isLessThanOrEqualTo(new BigDecimal("0.60"));
    }

    @Test
    void noPresumeDeConfianzaCuandoLosImportesNoCuadran() {
        // Ni la IA ni Tesseract cuadran: la revision tiene que quedar marcada como dudosa
        LineaExtraccion linea = linea("Lechuga romana", "7.40");
        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, null, null, "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("44.98"), List.of(linea)),
                        new BigDecimal("0.98"));
        ResultadoExtraccion tesseract =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("40.89"), new BigDecimal("4.09"),
                                new BigDecimal("44.98"), List.of(linea)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal()).combinar(vision, tesseract);

        assertThat(combinado.confianza()).isLessThanOrEqualTo(new BigDecimal("0.60"));
    }

    @Test
    void unaLecturaQueCuadraNoPierdeLaConfianzaContrastada() {
        LineaExtraccion linea = linea("Lechuga romana", "37.80");
        ResultadoExtraccion vision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, null, null, "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("41.58"), List.of(linea)),
                        new BigDecimal("0.40"));
        ResultadoExtraccion tesseract =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("37.80"), new BigDecimal("3.78"),
                                new BigDecimal("41.58"), List.of(linea)),
                        new BigDecimal("0.35"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal()).combinar(vision, tesseract);

        assertThat(combinado.confianza()).isEqualByComparingTo("0.85");
    }

    @Test
    void noDuplicaUnaLineaCuandoElOcrSeComeUnaLetra() {
        // "CEBOLLA BLANGA" es la misma línea que "CEBOLLA BLANCA", con una letra bailada
        LineaExtraccion vision = linea("CEBOLLA BLANCA", "3.60");
        LineaExtraccion tesseract = linea("CEBOLLA BLANGA", "3.80");

        ResultadoExtraccion lecturaVision =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "Distribuciones Ejemplo", null, null, null, "EUR",
                                new BigDecimal("3.60"), new BigDecimal("0.36"),
                                new BigDecimal("3.96"), List.of(vision)),
                        new BigDecimal("0.98"));
        ResultadoExtraccion lecturaLocal =
                new ResultadoExtraccion(
                        new ExtraccionDocumento(
                                "DISTRIBUCIONES EJEMPLO", null, null, null, "EUR",
                                new BigDecimal("3.80"), new BigDecimal("0.38"),
                                new BigDecimal("4.18"), List.of(tesseract)),
                        new BigDecimal("0.90"));

        ResultadoExtraccion combinado =
                new ExtractorHibrido(mock(ExtractorVision.class), textoLocal())
                        .combinar(lecturaVision, lecturaLocal);

        assertThat(combinado.extraccion().lineas()).hasSize(1);
        assertThat(combinado.extraccion().lineas().getFirst().descripcion())
                .isEqualTo("CEBOLLA BLANCA");
    }

    private LineaExtraccion linea(String descripcion, String importe) {
        return new LineaExtraccion(
                descripcion, BigDecimal.ONE, "kg", new BigDecimal(importe),
                new BigDecimal(importe), null, null, null);
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
