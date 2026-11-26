package com.chefstool.backend.adapter.out.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
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

    @Test
    void ampliaLasImagenesPequenasAntesDeLeerlas() {
        // Un reenvio de mensajeria llega a 768 px: el texto seria ilegible para Tesseract
        BufferedImage pequena = new BufferedImage(768, 1365, BufferedImage.TYPE_INT_RGB);

        assertThat(lector.rotaciones(pequena).getFirst().getWidth())
                .isGreaterThanOrEqualTo(1600);
    }

    @Test
    void reduceLasFotosEnormesParaNoTardarDeMas() {
        BufferedImage grande = new BufferedImage(4096, 3072, BufferedImage.TYPE_INT_RGB);

        assertThat(lector.rotaciones(grande).getFirst().getWidth()).isEqualTo(2200);
    }
    @Test
    void reconoceUnaLecturaCompletaAntesDeProbarVariantesMenosFiables() {
        String completo = """
            DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO S.L.
            LECHUGA ROMANA 010203 2026040282 2 2,50 5,00 10
            TOMATE PERA 0300034 2026040882 4 1,85 7,40 10
            CEBOLLA BLANCA 010012 20260440000001 3 1,20 3,60 10
            PIMIENTO VERDE 0100063 20260409131 2 3,40 6,80 10
            Base imponible: 37,80
            IVA 10%: 3,78
            TOTAL: 41,58
            """;

        assertThat(lector.lecturaCompleta(completo)).isTrue();
        assertThat(lector.lecturaCompleta("Solo contiene una cantidad 2,50 5,00")).isFalse();
    }

    @Test
    void añadeVariantesParaFotosGiradasLigeramenteSinCambiarElEscalado() {
        BufferedImage pequena = new BufferedImage(768, 1365, BufferedImage.TYPE_INT_RGB);

        assertThat(lector.rotaciones(pequena).size()).isGreaterThanOrEqualTo(7);
        assertThat(lector.rotaciones(pequena).getFirst().getWidth()).isGreaterThanOrEqualTo(1600);
    }

    @Test
    void leeLaOrientacionExifDeUnJpeg() throws Exception {
        java.io.ByteArrayOutputStream datos = new java.io.ByteArrayOutputStream();
        datos.write(new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe1});
        datos.write(new byte[] {0x00, 0x28});
        datos.write(new byte[] {'E', 'x', 'i', 'f', 0, 0, 'I', 'I', 42, 0, 8, 0, 0, 0});
        datos.write(new byte[] {1, 0});
        datos.write(new byte[] {0x12, 1, 3, 0, 1, 0, 0, 0, 6, 0, 0, 0});
        datos.write(new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        assertThat(lector.orientacionExif(datos.toByteArray())).isEqualTo(6);
    }

    @Test
    void distingueUnaImagenAbribleDeUnaQueNo() throws Exception {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "png", png);

        byte[] webp = new byte[] {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '
        };

        assertThat(lector.sePuedeDecodificar(png.toByteArray())).isTrue();
        assertThat(lector.sePuedeDecodificar(webp)).isFalse();
        assertThat(lector.sePuedeDecodificar(new byte[0])).isFalse();
    }

    @Test
    void elContrasteLocalIgualaLaZonaEnSombraConLaZonaIluminada() {
        // Foto con la sombra de la mano: la mitad izquierda a 60 de gris y la derecha a 200
        BufferedImage conSombra = new BufferedImage(400, 200, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 400; x++) {
                int gris = x < 200 ? 60 : 200;
                conSombra.setRGB(x, y, (gris << 16) | (gris << 8) | gris);
            }
        }

        BufferedImage normalizada = lector.contrasteLocal(conSombra);

        assertThat(normalizada.getWidth()).isEqualTo(400);
        assertThat(normalizada.getHeight()).isEqualTo(200);
        // El estiramiento global dejaria 60 y 200; el local lleva las dos zonas al gris medio
        int izquierda = normalizada.getRGB(50, 100) & 0xff;
        int derecha = normalizada.getRGB(350, 100) & 0xff;
        assertThat(Math.abs(izquierda - derecha)).isLessThan(30);
        assertThat(izquierda).isBetween(100, 160);
    }

    @Test
    void elContrasteLocalMantieneElTextoOscuroSobreFondoClaro() {
        BufferedImage hoja = new BufferedImage(300, 60, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 60; y++) {
            for (int x = 0; x < 300; x++) {
                hoja.setRGB(x, y, (250 << 16) | (250 << 8) | 250);
            }
        }
        // Un trazo fino, como el de una linea de texto impresa
        for (int y = 29; y <= 31; y++) {
            for (int x = 20; x < 280; x++) {
                hoja.setRGB(x, y, 0);
            }
        }

        BufferedImage normalizada = lector.contrasteLocal(hoja);

        assertThat(normalizada.getRGB(150, 30) & 0xff).isLessThan(80);
        // El fondo liso queda en el gris medio, sin ruido inventado
        assertThat(normalizada.getRGB(20, 10) & 0xff).isBetween(100, 160);
    }
}
