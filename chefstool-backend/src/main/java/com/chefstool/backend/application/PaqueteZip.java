package com.chefstool.backend.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Junta varios archivos en un ZIP con nombres legibles (fecha_nombre), sin acentos que se
 * rompen al descomprimir en Windows y sin entradas repetidas.
 */
public final class PaqueteZip {

    public record Archivo(String nombre, byte[] contenido) {}

    private PaqueteZip() {}

    public static byte[] empaquetar(List<Archivo> archivos) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(salida)) {
            Set<String> usados = new HashSet<>();
            for (Archivo archivo : archivos) {
                if (archivo.contenido() == null || archivo.contenido().length == 0) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(sinRepetir(nombreSeguro(archivo.nombre()), usados)));
                zip.write(archivo.contenido());
                zip.closeEntry();
            }
            zip.finish();
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo preparar el ZIP", ex);
        }
    }

    /** Nombre de archivo seguro dentro del ZIP: sin rutas ni caracteres problematicos. */
    public static String nombreSeguro(String nombre) {
        String limpio = nombre == null ? "documento" : nombre.trim();
        limpio = Normalizer.normalize(limpio, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        limpio = limpio.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_");
        return limpio.isBlank() ? "documento" : limpio;
    }

    private static String sinRepetir(String entrada, Set<String> usados) {
        if (usados.add(entrada)) {
            return entrada;
        }
        int punto = entrada.lastIndexOf('.');
        String base = punto > 0 ? entrada.substring(0, punto) : entrada;
        String extension = punto > 0 ? entrada.substring(punto) : "";
        int numero = 2;
        String candidato = "%s_(%d)%s".formatted(base, numero, extension);
        while (!usados.add(candidato)) {
            numero++;
            candidato = "%s_(%d)%s".formatted(base, numero, extension);
        }
        return candidato;
    }
}
