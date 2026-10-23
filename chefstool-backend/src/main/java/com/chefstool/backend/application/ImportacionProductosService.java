package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Importa el catalogo desde un CSV (el que exporta Excel con "Guardar como CSV").
 *
 * <p>Es tolerante: acepta punto y coma o coma como separador, BOM de Excel, acentos y
 * cabecera opcional. La primera fila se usa como cabecera si contiene "nombre".
 */
@Service
public class ImportacionProductosService {

    private static final int MAXIMO_FILAS = 1000;

    private final ProductoService productoService;

    public ImportacionProductosService(ProductoService productoService) {
        this.productoService = productoService;
    }

    public record Resumen(int creados, int omitidos, List<String> errores) {}

    /**
     * Sin transaccion unica a proposito: cada producto se guarda en su propia transaccion,
     * asi un duplicado no invalida el resto de la importacion.
     */
    public Resumen importar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Adjunta un archivo CSV");
        }
        return importar(leer(archivo));
    }

    /** Separado para poder probarlo sin MultipartFile. */
    Resumen importar(String contenido) {
        List<String> lineas = contenido.lines().toList();
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        List<String> errores = new ArrayList<>();
        int creados = 0;
        int omitidos = 0;
        boolean primera = true;

        for (String lineaCruda : lineas) {
            String linea = lineaCruda.strip();
            if (linea.isEmpty()) {
                continue;
            }

            String[] campos = separar(linea);
            if (primera) {
                primera = false;
                if (esCabecera(campos)) {
                    continue;
                }
            }

            String nombre = limpiar(campo(campos, 0));
            if (nombre.isBlank()) {
                omitidos++;
                continue;
            }
            if (creados + omitidos >= MAXIMO_FILAS) {
                errores.add("Se han procesado las primeras " + MAXIMO_FILAS + " filas");
                break;
            }

            try {
                productoService.crear(
                        new ProductoRequest(
                                recortar(nombre, 120),
                                recortar(limpiar(campo(campos, 1)), 80),
                                unidad(limpiar(campo(campos, 2)))));
                creados++;
            } catch (IllegalArgumentException ex) {
                // Duplicado o dato invalido: se cuenta y se sigue con el resto
                omitidos++;
                if (errores.size() < 20) {
                    errores.add(nombre + ": " + ex.getMessage());
                }
            }
        }

        return new Resumen(creados, omitidos, errores);
    }

    private String leer(MultipartFile archivo) {
        try (BufferedReader lector =
                new BufferedReader(
                        new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {
            return lector.lines().reduce("", (a, b) -> a + b + "\n");
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + ex.getMessage());
        }
    }

    private String[] separar(String linea) {
        String separador = linea.contains(";") ? ";" : ",";
        // Respetamos las comillas dobles para los campos con separador dentro
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean entreComillas = false;
        for (char caracter : linea.toCharArray()) {
            if (caracter == '"') {
                entreComillas = !entreComillas;
            } else if (separador.equals(String.valueOf(caracter)) && !entreComillas) {
                campos.add(actual.toString());
                actual.setLength(0);
            } else {
                actual.append(caracter);
            }
        }
        campos.add(actual.toString());
        return campos.toArray(new String[0]);
    }

    private boolean esCabecera(String[] campos) {
        String primero = limpiar(campo(campos, 0)).toLowerCase(Locale.ROOT);
        return primero.contains("nombre") || primero.contains("producto") || primero.contains("articulo");
    }

    private String campo(String[] campos, int indice) {
        return indice < campos.length ? campos[indice] : "";
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        // Excel a veces antepone el BOM y deja comillas sueltas
        return valor.replace("\uFEFF", "").replace("\"", "").strip();
    }

    private String unidad(String valor) {
        String unidad = valor.toLowerCase(Locale.ROOT);
        if (unidad.isBlank()) {
            return null;
        }
        return switch (unidad) {
            case "kilos", "kilogramos", "kilo", "kilogramo" -> "kg";
            case "litros", "litro" -> "l";
            case "gramos", "gramo" -> "g";
            case "mililitros", "mililitro" -> "ml";
            case "unidades", "unidad" -> "ud";
            default -> recortar(unidad, 20);
        };
    }

    private String recortar(String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip();
        return limpio.length() > maximo ? limpio.substring(0, maximo) : limpio;
    }
}
