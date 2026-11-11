package com.chefstool.backend.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LectorImagenTest {

    private final LectorImagen lector = new LectorImagen("tesseract-no-instalado", "spa");

    @Test
    void prefiereElTextoConFilasCompletasFrenteAlDeColumnasPartidas() {
        // Asi lee Tesseract con la segmentacion automatica: cada columna en una linea distinta
        String columnasPartidas =
                """
                DISTRIBUCIONES MAKRO SL
                Descripcion
                Aceite de oliva virgen extra 5L
                Harina de trigo 25kg
                Sal gruesa 10kg
                """;

        // Asi lee con segmentacion por bloques: la fila del albaran queda entera
        String filasCompletas =
                """
                DISTRIBUCIONES MAKRO SL ALBARAN N: A-2026/0457
                Descripcion Cant. Precio Importe
                Aceite de oliva virgen extra 5L 2 24,50 49,00
                Harina de trigo 25kg 3 18,90 56,70
                Sal gruesa 10kg 1 6,40 6,40
                """;

        assertThat(lector.puntuar(filasCompletas))
                .isGreaterThan(lector.puntuar(columnasPartidas));
    }

    @Test
    void sinTextoNoPuntua() {
        assertThat(lector.puntuar(null)).isZero();
        assertThat(lector.puntuar("   ")).isZero();
    }
}
