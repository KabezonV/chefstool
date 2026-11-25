package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
            throw new IllegalArgumentException("Adjunta un archivo CSV o Excel");
        }

        String nombre = archivo.getOriginalFilename() == null
                ? ""
                : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            return importarFilasExcel(archivo, nombre.endsWith(".xls"));
        }
        return importar(leer(archivo));
    }

    private Resumen importarFilasExcel(MultipartFile archivo, boolean xls) {
        try (java.io.InputStream entrada = archivo.getInputStream();
                Workbook libro = xls ? new HSSFWorkbook(entrada) : new XSSFWorkbook(entrada)) {
            StringBuilder contenido = new StringBuilder();
            for (int s = 0; s < libro.getNumberOfSheets(); s++) {
                Sheet hoja = libro.getSheetAt(s);
                for (Row fila : hoja) {
                    if (fila == null) {
                        continue;
                    }
                    List<String> campos = new ArrayList<>();
                    int ultima = Math.max(fila.getLastCellNum(), 0);
                    for (int c = 0; c < ultima; c++) {
                        campos.add(valorCelda(fila.getCell(c)));
                    }
                    contenido.append(String.join(";", campos)).append('\n');
                }
                if (s + 1 < libro.getNumberOfSheets()) {
                    contenido.append('\n');
                }
            }
            if (contenido.isEmpty()) {
                throw new IllegalArgumentException("El archivo Excel está vacío");
            }
            return importar(contenido.toString());
        } catch (IOException ex) {
            throw new IllegalArgumentException("No se pudo leer el archivo Excel: " + ex.getMessage());
        } catch (org.apache.poi.EmptyFileException ex) {
            throw new IllegalArgumentException("El archivo Excel está vacío o no es válido");
        }
    }

    private String valorCelda(Cell celda) {
        if (celda == null) {
            return "";
        }
        return switch (celda.getCellType()) {
            case STRING -> celda.getStringCellValue();
            case NUMERIC -> new java.text.DecimalFormat("0.################",
                            java.text.DecimalFormatSymbols.getInstance(Locale.ROOT))
                    .format(celda.getNumericCellValue());
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case FORMULA -> celda.toString();
            default -> "";
        };
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
