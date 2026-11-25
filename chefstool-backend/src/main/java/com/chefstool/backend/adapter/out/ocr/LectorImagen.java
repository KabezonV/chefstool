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
     * Si la imagen se puede abrir. Java no entiende WebP ni HEIC, y el navegador deja elegirlos
     * porque el selector acepta "image/*".
     */
    public boolean sePuedeDecodificar(byte[] imagen) {
        if (imagen == null || imagen.length == 0) {
            return false;
        }
        try {
            return ImageIO.read(new ByteArrayInputStream(imagen)) != null;
        } catch (IOException ex) {
            return false;
        }
    }

    /** Lee la orientacion EXIF de un JPEG sin depender de librerias externas. */
    int orientacionExif(byte[] imagen) {
        if (imagen == null || imagen.length < 12) {
            return 1;
        }
        int i = 2;
        while (i + 4 < imagen.length) {
            int marcador = ((imagen[i] & 0xff) << 8) | (imagen[i + 1] & 0xff);
            if (marcador == 0xffd8) {
                i += 2;
                continue;
            }
            if (marcador == 0xffd9) {
                break;
            }
            int longitud = ((imagen[i + 2] & 0xff) << 8) | (imagen[i + 3] & 0xff);
            if (longitud < 2 || i + 2 + longitud > imagen.length) {
                break;
            }
            int datos = i + 4;
            if (marcador == 0xffe1 && longitud >= 8
                    && imagen.length >= datos + 6
                    && imagen[datos] == 'E' && imagen[datos + 1] == 'x'
                    && imagen[datos + 2] == 'i' && imagen[datos + 3] == 'f'
                    && imagen[datos + 4] == 0 && imagen[datos + 5] == 0) {
                int tiff = datos + 6;
                if (tiff + 8 > imagen.length) {
                    return 1;
                }
                boolean little = imagen[tiff] == 'I' && imagen[tiff + 1] == 'I';
                boolean big = imagen[tiff] == 'M' && imagen[tiff + 1] == 'M';
                if (!little && !big) {
                    return 1;
                }
                int ifd0 = leer32(imagen, tiff + 4, little, tiff);
                int ifd = tiff + ifd0;
                if (ifd + 2 > imagen.length) {
                    return 1;
                }
                int entradas = leer16(imagen, ifd, little);
                for (int j = 0; j < entradas; j++) {
                    int entrada = ifd + 2 + j * 12;
                    if (entrada + 12 > imagen.length) {
                        break;
                    }
                    int etiqueta = leer16(imagen, entrada, little);
                    if (etiqueta == 274) {
                        int tipo = leer16(imagen, entrada + 2, little);
                        int cantidad = leer32(imagen, entrada + 4, little, 0);
                        if (cantidad == 1) {
                            if (tipo == 3) {
                                return leer16(imagen, entrada + 8, little);
                            }
                            if (tipo == 4) {
                                return leer32(imagen, entrada + 8, little, 0);
                            }
                        }
                    }
                }
            }
            i += 2 + longitud;
        }
        return 1;
    }

    private int leer16(byte[] datos, int offset, boolean little) {
        if (offset + 2 > datos.length) {
            return 0;
        }
        int b0 = datos[offset] & 0xff;
        int b1 = datos[offset + 1] & 0xff;
        return little ? b0 | (b1 << 8) : (b0 << 8) | b1;
    }

    private int leer32(byte[] datos, int offset, boolean little, int baseTiff) {
        if (offset + 4 > datos.length) {
            return 0;
        }
        int valor;
        if (little) {
            valor = datos[offset] & 0xff
                    | ((datos[offset + 1] & 0xff) << 8)
                    | ((datos[offset + 2] & 0xff) << 16)
                    | ((datos[offset + 3] & 0xff) << 24);
        } else {
            valor = ((datos[offset] & 0xff) << 24)
                    | ((datos[offset + 1] & 0xff) << 16)
                    | ((datos[offset + 2] & 0xff) << 8)
                    | (datos[offset + 3] & 0xff);
        }
        return baseTiff == 0 ? valor : valor;
    }

    private BufferedImage orientarExif(BufferedImage original, int orientacion) {
        return switch (orientacion) {
            case 2 -> transformarExif(original, 2);
            case 3 -> transformarExif(original, 3);
            case 6 -> transformarExif(original, 6);
            case 8 -> transformarExif(original, 8);
            default -> original;
        };
    }

    private BufferedImage transformarExif(BufferedImage original, int tipo) {
        int ancho = original.getWidth();
        int alto = original.getHeight();
        boolean giro = tipo == 6 || tipo == 8;
        BufferedImage destino = new BufferedImage(
                giro ? alto : ancho,
                giro ? ancho : alto,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = destino.createGraphics();
        AffineTransform transformacion = new AffineTransform();
        if (tipo == 2) {
            transformacion.translate(ancho, 0);
            transformacion.scale(-1, 1);
        } else if (tipo == 3) {
            transformacion.translate(ancho, alto);
            transformacion.rotate(Math.PI);
        } else if (tipo == 6) {
            transformacion.translate(alto, 0);
            transformacion.rotate(Math.toRadians(90));
        } else if (tipo == 8) {
            transformacion.translate(0, ancho);
            transformacion.rotate(Math.toRadians(-90));
        }
        g.drawImage(original, transformacion, null);
        g.dispose();
        return destino;
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
            if (original != null) {
                original = orientarExif(original, orientacionExif(imagen));
            }
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

        List<BufferedImage> variantes = new ArrayList<>();
        boolean usarArchivoOriginal = original.getWidth() >= ANCHO_MAXIMO;
        if (usarArchivoOriginal) {
            variantes.add(aRgb(original));
        }
        variantes.addAll(rotaciones(original));
        for (int i = 0; i < variantes.size(); i++) {
            Optional<String> texto =
                    i == 0 && usarArchivoOriginal
                            ? leerConTesseractArchivo(imagen, PSM_BLOQUE)
                            : leerConTesseract(aPng(variantes.get(i)), PSM_BLOQUE);
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

        if (mejorPuntuacion <= 0) {
            return Optional.empty();
        }
        log.debug("Texto OCR seleccionado:\n{}", mejorTexto);
        return Optional.of(mejorTexto);
    }

    /** Endereza giros leves y prueba orientaciones completas con un tamano razonable. */
    List<BufferedImage> rotaciones(BufferedImage original) {
        BufferedImage escalada = escalar(original);
        BufferedImage contrastada = mejorarContraste(escalada);
        List<BufferedImage> variantes = new ArrayList<>();
        variantes.add(girar(escalada, 0));
        for (double grados : new double[] {-2.5, 2.5, -5, 5, 90, 180, 270}) {
            variantes.add(girar(escalada, grados));
        }
        variantes.add(girar(contrastada, 0));
        return variantes;
    }

    private BufferedImage girar(BufferedImage original, double grados) {
        BufferedImage escalada = original;
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
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
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

    /** Mejora sombras y fondo sin convertir el texto en bordes artificiales. */
    private BufferedImage mejorarContraste(BufferedImage original) {
        BufferedImage origen = aRgb(original);
        int ancho = origen.getWidth();
        int alto = origen.getHeight();
        int minimo = 255;
        int maximo = 0;

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int rgb = origen.getRGB(x, y);
                int rojo = (rgb >> 16) & 0xff;
                int verde = (rgb >> 8) & 0xff;
                int azul = rgb & 0xff;
                int luminancia = (299 * rojo + 587 * verde + 114 * azul) / 1000;
                minimo = Math.min(minimo, luminancia);
                maximo = Math.max(maximo, luminancia);
            }
        }

        if (maximo <= minimo + 20) {
            return origen;
        }

        BufferedImage destino =
                new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int rgb = origen.getRGB(x, y);
                int rojo = (rgb >> 16) & 0xff;
                int verde = (rgb >> 8) & 0xff;
                int azul = rgb & 0xff;
                int luminancia = (299 * rojo + 587 * verde + 114 * azul) / 1000;
                int nuevo =
                        Math.max(
                                0,
                                Math.min(
                                        255,
                                        (luminancia - minimo) * 255 / (maximo - minimo)));
                int rgbNuevo = (nuevo << 16) | (nuevo << 8) | nuevo;
                destino.setRGB(x, y, rgbNuevo);
            }
        }
        return destino;
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

    boolean lecturaCompleta(String texto) {
        if (texto == null) {
            return false;
        }
        Matcher filas = FILA_ALBARAN.matcher(texto);
        int filasComplejas = 0;
        while (filas.find()) {
            filasComplejas++;
        }
        String normalizado = texto.toLowerCase(java.util.Locale.ROOT);
        return filasComplejas >= 4
                && normalizado.contains("base imponible")
                && normalizado.contains("iva")
                && normalizado.contains("total");
    }

    private Optional<String> leerConTesseract(byte[] png, int psm) {
        return ejecutar(png, psm);
    }

    private Optional<String> leerConTesseractArchivo(byte[] archivo, int psm) {
        String extension = archivo.length >= 2
                        && (archivo[0] & 0xff) == 0xff
                        && (archivo[1] & 0xff) == 0xd8
                ? ".jpg"
                : ".png";
        return ejecutarArchivo(archivo, psm, extension);
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
        return ejecutarArchivo(imagenPng, psm, ".png");
    }

    private Optional<String> ejecutarArchivo(byte[] archivo, int psm, String extension) {
        Path temporal = null;
        try {
            temporal = Files.createTempFile("chefstool-ocr-", extension);
            Files.write(temporal, archivo);

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
