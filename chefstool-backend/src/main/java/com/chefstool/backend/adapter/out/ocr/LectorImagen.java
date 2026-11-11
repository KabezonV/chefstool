package com.chefstool.backend.adapter.out.ocr;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OCR de imagenes usando el binario de Tesseract si esta instalado.
 *
 * <p>Se invoca el ejecutable con ProcessBuilder en lugar de enlazar librerias nativas:
 * si no esta disponible, la aplicacion sigue funcionando y avisa en el documento.
 */
@Component
public class LectorImagen {

    private static final Logger log = LoggerFactory.getLogger(LectorImagen.class);

    /** Si el texto de la primera pasada ya trae importes y palabras de albaran, no se gira. */
    private static final int PUNTUACION_SUFICIENTE = 12;

    /** Las fotos de movil vienen enormes; con 2200 px de ancho Tesseract va mas rapido. */
    private static final int ANCHO_MAXIMO = 2200;

    /**
     * Por debajo de este ancho el texto sale demasiado pequeno para Tesseract. Pasa con las
     * imagenes reenviadas por mensajeria (por ejemplo, 768 px): se amplian hasta este tamano.
     */
    private static final int ANCHO_MINIMO = 1600;

    /** Tope de ampliacion, para no inflar una imagen diminuta hasta el absurdo. */
    private static final double FACTOR_MAXIMO = 3.0;

    /**
     * Segmentacion por bloques: es la que mantiene cada fila del albaran en una sola linea.
     * Con la segmentacion automatica (3) las columnas se parten y luego no hay forma de leerlas.
     */
    private static final int PSM_BLOQUE = 6;

    /** Segmentacion automatica, solo como rescate si la de bloque no encuentra nada. */
    private static final int PSM_AUTOMATICO = 3;

    private static final Pattern IMPORTE = Pattern.compile("\\d{1,4}[.,]\\d{2}\\b");

    /** Dos importes seguidos en la misma linea: la forma de una fila de albaran. */
    private static final Pattern FILA_ALBARAN = Pattern.compile("\\d[.,]\\d{2,4}\\s+\\d[.,]\\d{2,4}\\b");

    private static final Pattern PALABRAS_CLAVE =
            Pattern.compile(
                    "\\b(total|iva|base|importe|fecha|albar[aá]n|factura|pedido|cantidad|precio|dto|descuento)\\b",
                    Pattern.CASE_INSENSITIVE);

    private final String comando;
    private final String idioma;

    public LectorImagen(
            @Value("${chefstool.ocr.tesseract:tesseract}") String comando,
            @Value("${chefstool.ocr.idioma:spa}") String idioma) {
        this.comando = comando;
        this.idioma = idioma;
    }

    public boolean disponible() {
        try {
            Process proceso =
                    new ProcessBuilder(comando, "--version")
                            .redirectErrorStream(true)
                            .start();
            boolean termino = proceso.waitFor(5, TimeUnit.SECONDS);
            return termino && proceso.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Lee la imagen probando orientaciones: las fotos de movil llegan giradas muy a menudo
     * y Tesseract no las endereza solo. Se queda con la lectura que mas se parece a un albaran.
     */
    public Optional<String> extraerTexto(byte[] imagen) {
        if (imagen == null || imagen.length == 0 || !disponible()) {
            return Optional.empty();
        }

        BufferedImage original;
        try {
            original = ImageIO.read(new ByteArrayInputStream(imagen));
        } catch (IOException ex) {
            log.warn("No se pudo decodificar la imagen: {}", ex.getMessage());
            return Optional.empty();
        }
        if (original == null) {
            return Optional.empty();
        }

        String mejorTexto = null;
        int mejorPuntuacion = -1;
        int mejorIndice = -1;

        List<BufferedImage> variantes = rotaciones(original);
        for (int i = 0; i < variantes.size(); i++) {
            Optional<String> texto = leerConTesseract(aPng(variantes.get(i)), PSM_BLOQUE);
            if (texto.isEmpty()) {
                continue;
            }
            int puntuacion = puntuar(texto.get());
            if (puntuacion > mejorPuntuacion) {
                mejorPuntuacion = puntuacion;
                mejorTexto = texto.get();
                mejorIndice = i;
            }
            if (i == 0 && puntuacion >= PUNTUACION_SUFICIENTE) {
                break;
            }
        }

        // Rescate: hay documentos (columnas anchas, tablas con bordes) que la segmentacion por
        // bloques corta mal. Se prueba la automatica sobre la orientacion que mejor puntuo.
        if (mejorIndice >= 0 && mejorPuntuacion < PUNTUACION_SUFICIENTE) {
            Optional<String> rescate =
                    leerConTesseract(aPng(variantes.get(mejorIndice)), PSM_AUTOMATICO);
            if (rescate.isPresent() && puntuar(rescate.get()) > mejorPuntuacion) {
                mejorTexto = rescate.get();
                mejorPuntuacion = puntuar(mejorTexto);
            }
        }

        return mejorPuntuacion <= 0 ? Optional.empty() : Optional.of(mejorTexto);
    }

    /** Original y giros de 90, 180 y 270 grados, escalados a un tamano razonable. */
    List<BufferedImage> rotaciones(BufferedImage original) {
        List<BufferedImage> variantes = new ArrayList<>();
        for (int grados : new int[] {0, 90, 180, 270}) {
            variantes.add(girar(original, grados));
        }
        return variantes;
    }

    private BufferedImage girar(BufferedImage original, int grados) {
        BufferedImage escalada = escalar(original);
        if (grados == 0) {
            return escalada;
        }

        double radianes = Math.toRadians(grados);
        int ancho = escalada.getWidth();
        int alto = escalada.getHeight();
        boolean cuartoDeVuelta = grados == 90 || grados == 270;

        BufferedImage destino =
                new BufferedImage(
                        cuartoDeVuelta ? alto : ancho,
                        cuartoDeVuelta ? ancho : alto,
                        BufferedImage.TYPE_INT_RGB);

        Graphics2D g = destino.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        AffineTransform transformacion = new AffineTransform();
        transformacion.translate(destino.getWidth() / 2.0, destino.getHeight() / 2.0);
        transformacion.rotate(radianes);
        transformacion.translate(-escalada.getWidth() / 2.0, -escalada.getHeight() / 2.0);
        g.drawImage(escalada, transformacion, null);
        g.dispose();
        return destino;
    }

    private BufferedImage escalar(BufferedImage original) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        if (ancho > ANCHO_MAXIMO) {
            double factor = (double) ANCHO_MAXIMO / ancho;
            int nuevoAlto = Math.max((int) (alto * factor), 1);
            BufferedImage destino =
                    new BufferedImage(ANCHO_MAXIMO, nuevoAlto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = destino.createGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original, 0, 0, ANCHO_MAXIMO, nuevoAlto, null);
            g.dispose();
            return destino;
        }
        if (ancho < ANCHO_MINIMO) {
            double factor = Math.min(FACTOR_MAXIMO, (double) ANCHO_MINIMO / ancho);
            int nuevoAncho = Math.max((int) (ancho * factor), 1);
            int nuevoAlto = Math.max((int) (alto * factor), 1);
            BufferedImage destino =
                    new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = destino.createGraphics();
            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
            g.dispose();
            return destino;
        }
        return aRgb(original);
    }

    private BufferedImage aRgb(BufferedImage original) {
        if (original.getType() == BufferedImage.TYPE_INT_RGB) {
            return original;
        }
        BufferedImage destino =
                new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        g.drawImage(original, 0, 0, null);
        g.dispose();
        return destino;
    }

    /** Cuanto se parece el texto a un albaran: importes, palabras clave y letras. */
    int puntuar(String texto) {
        if (texto == null || texto.isBlank()) {
            return 0;
        }
        int puntuacion = 0;

        Matcher importes = IMPORTE.matcher(texto);
        while (importes.find()) {
            puntuacion += 3;
            if (puntuacion > 40) {
                break;
            }
        }

        Matcher palabras = PALABRAS_CLAVE.matcher(texto);
        while (palabras.find()) {
            puntuacion += 2;
            if (puntuacion > 60) {
                break;
            }
        }

        Matcher filas = FILA_ALBARAN.matcher(texto);
        while (filas.find()) {
            puntuacion += 4;
            if (puntuacion > 80) {
                break;
            }
        }

        long letras = texto.chars().filter(Character::isLetter).count();
        long total = texto.length();
        if (total > 0 && letras * 100 / total >= 40) {
            puntuacion += 2;
        }
        return puntuacion;
    }

    private Optional<String> leerConTesseract(byte[] png, int psm) {
        return ejecutar(png, psm);
    }

    private byte[] aPng(BufferedImage imagen) {
        try {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            ImageIO.write(imagen, "png", salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo preparar la imagen para OCR", ex);
        }
    }


    /** Invoca Tesseract sobre el PNG preparado. */
    private Optional<String> ejecutar(byte[] imagenPng, int psm) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("chefstool-ocr-", ".png");
            Files.write(temporal, imagenPng);

            Process proceso =
                    new ProcessBuilder(
                                    comando,
                                    temporal.toString(),
                                    "stdout",
                                    "-l",
                                    idioma,
                                    "--psm",
                                    String.valueOf(psm))
                            .redirectErrorStream(false)
                            .start();

            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean termino = proceso.waitFor(120, TimeUnit.SECONDS);

            if (!termino || proceso.exitValue() != 0) {
                log.warn("Tesseract termino con codigo {}", termino ? proceso.exitValue() : -1);
                return Optional.empty();
            }
            return Optional.of(salida);
        } catch (IOException ex) {
            log.warn("No se pudo ejecutar Tesseract: {}", ex.getMessage());
            return Optional.empty();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } finally {
            if (temporal != null) {
                try {
                    Files.deleteIfExists(temporal);
                } catch (IOException ex) {
                    log.debug("No se pudo borrar el temporal de OCR", ex);
                }
            }
        }
    }
}
