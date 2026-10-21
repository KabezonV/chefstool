package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.model.Producto;
import com.chefstool.backend.domain.model.Proveedor;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ProductoRepository;
import com.chefstool.backend.domain.port.out.ProveedorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Interpreta el texto de un albaran o factura española: busca proveedor, fecha,
 * numero, lineas e importes, y empareja las lineas con el catalogo de productos.
 *
 * <p>Es deliberadamente heuristico: el resultado se revisa en pantalla antes de
 * confirmar la compra.
 */
@Service
public class ParserAlbaran {

    private static final Pattern FECHA =
            Pattern.compile("(\\d{1,2})[/\\-.]{1}(\\d{1,2})[/\\-.]{1}(\\d{2,4})");

    private static final Pattern NUMERO_DOCUMENTO =
            Pattern.compile(
                    "(?:N[º°o]?\\.?|NUM(?:ERO)?|FACTURA|ALBAR[AÁ]N)\\s*[:.]?\\s*([A-Z0-9][A-Z0-9\\-/]{2,})",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern LINEA_CON_UNIDAD =
            Pattern.compile(
                    "^(.+?)\\s+(\\d{1,4}(?:[.,]\\d{1,3})?)\\s*(kg|g|l|ml|ud|uds|un|doc|caja|bolsa)?\\s+"
                            + "(\\d{1,4}[.,]\\d{2,4})\\s+(\\d{1,6}[.,]\\d{2})$",
                    Pattern.CASE_INSENSITIVE);

    private static final Pattern LINEA_SIN_UNIDAD =
            Pattern.compile(
                    "^(.+?)\\s+(\\d{1,4}[.,]\\d{2,4})\\s+(\\d{1,6}[.,]\\d{2})$");

    private static final Pattern IMPORTE =
            Pattern.compile("(-?\\d{1,3}(?:\\.\\d{3})*,\\d{2}|-?\\d+[.,]\\d{2})");

    private static final Set<String> PALABRAS_IGNORADAS =
            Set.of("caja", "cajas", "bolsa", "bolsas", "pack", "bandeja", "de", "del", "la", "el");

    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;

    public ParserAlbaran(
            ProductoRepository productoRepository, ProveedorRepository proveedorRepository) {
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
    }

    public ResultadoExtraccion interpretar(String texto, UUID organizacionId) {
        List<String> lineas = texto == null
                ? List.of()
                : Arrays.stream(texto.split("\\R"))
                        .map(String::trim)
                        .filter(linea -> !linea.isBlank())
                        .toList();

        if (lineas.isEmpty()) {
            return new ResultadoExtraccion(extraccionVacia(), BigDecimal.ZERO);
        }

        double confianza = 0;

        Optional<Proveedor> proveedor = buscarProveedor(lineas, organizacionId);
        if (proveedor.isPresent()) {
            confianza += 0.35;
        }

        Optional<LocalDate> fecha = buscarFecha(texto);
        if (fecha.isPresent()) {
            confianza += 0.15;
        }

        String numero = buscarNumero(texto);
        if (numero != null) {
            confianza += 0.05;
        }

        List<LineaExtraccion> articulos = buscarLineas(lineas, organizacionId);
        if (!articulos.isEmpty()) {
            confianza += 0.3;
        }

        BigDecimal total = buscarImporte(lineas, "TOTAL", "TOTAL FACTURA", "TOTAL ALBARAN");
        BigDecimal iva = buscarImporte(lineas, "IVA", "I.V.A");
        BigDecimal neto = buscarImporte(lineas, "BASE", "SUBTOTAL", "BASE IMPONIBLE");

        if (total != null) {
            confianza += 0.15;
        }

        BigDecimal sumaLineas =
                articulos.stream()
                        .map(LineaExtraccion::importe)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netoFinal = neto != null ? neto : sumaLineas;
        BigDecimal totalFinal =
                total != null ? total : netoFinal.add(iva == null ? BigDecimal.ZERO : iva);

        ExtraccionDocumento extraccion =
                new ExtraccionDocumento(
                        proveedor.map(Proveedor::getNombre).orElse(nombreProveedorProbable(lineas)),
                        proveedor.map(Proveedor::getId).orElse(null),
                        numero,
                        fecha.orElse(null),
                        "EUR",
                        netoFinal.setScale(2, RoundingMode.HALF_UP),
                        iva == null ? BigDecimal.ZERO : iva,
                        totalFinal.setScale(2, RoundingMode.HALF_UP),
                        articulos);

        return new ResultadoExtraccion(
                extraccion,
                BigDecimal.valueOf(Math.min(0.95, confianza)).setScale(2, RoundingMode.HALF_UP));
    }

    private ExtraccionDocumento extraccionVacia() {
        return new ExtraccionDocumento(
                null, null, null, null, "EUR",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
    }

    private Optional<Proveedor> buscarProveedor(List<String> lineas, UUID organizacionId) {
        List<Proveedor> proveedores =
                proveedorRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionId);
        if (proveedores.isEmpty()) {
            return Optional.empty();
        }

        String primerasLineas = normalizar(String.join(" ", lineas.subList(0, Math.min(6, lineas.size()))));

        return proveedores.stream()
                .filter(proveedor -> primerasLineas.contains(normalizar(proveedor.getNombre())))
                .findFirst()
                .or(() ->
                        proveedores.stream()
                                .filter(
                                        proveedor ->
                                                coincidenciaParcial(
                                                        primerasLineas,
                                                        normalizar(proveedor.getNombre())))
                                .findFirst());
    }

    private Optional<LocalDate> buscarFecha(String texto) {
        Matcher matcher = FECHA.matcher(texto);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            int dia = Integer.parseInt(matcher.group(1));
            int mes = Integer.parseInt(matcher.group(2));
            int anio = Integer.parseInt(matcher.group(3));
            if (anio < 100) {
                anio += 2000;
            }
            return Optional.of(LocalDate.of(anio, mes, dia));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private String buscarNumero(String texto) {
        Matcher matcher = NUMERO_DOCUMENTO.matcher(texto);
        return matcher.find() ? matcher.group(1) : null;
    }

    private List<LineaExtraccion> buscarLineas(List<String> lineas, UUID organizacionId) {
        List<Producto> catalogo =
                productoRepository.findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(
                        organizacionId);
        List<LineaExtraccion> resultado = new ArrayList<>();

        for (String linea : lineas) {
            if (esRuido(linea)) {
                continue;
            }

            LineaExtraccion articulo = interpretarLinea(linea, catalogo);
            if (articulo != null) {
                resultado.add(articulo);
            }
        }

        return resultado;
    }

    private LineaExtraccion interpretarLinea(String linea, List<Producto> catalogo) {
        Matcher conUnidad = LINEA_CON_UNIDAD.matcher(linea);
        if (conUnidad.matches()) {
            BigDecimal cantidad = numero(conUnidad.group(2));
            BigDecimal precio = numero(conUnidad.group(4));
            BigDecimal importe = numero(conUnidad.group(5));
            if (cuadra(cantidad, precio, importe)) {
                return construir(
                        conUnidad.group(1),
                        cantidad,
                        conUnidad.group(3),
                        precio,
                        importe,
                        catalogo);
            }
        }

        Matcher sinUnidad = LINEA_SIN_UNIDAD.matcher(linea);
        if (sinUnidad.matches()) {
            BigDecimal precio = numero(sinUnidad.group(2));
            BigDecimal importe = numero(sinUnidad.group(3));
            if (precio.signum() > 0 && importe.signum() > 0) {
                BigDecimal cantidad = importe.divide(precio, 3, RoundingMode.HALF_UP);
                return construir(
                        sinUnidad.group(1), cantidad, null, precio, importe, catalogo);
            }
        }

        return null;
    }

    private LineaExtraccion construir(
            String descripcion,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precio,
            BigDecimal importe,
            List<Producto> catalogo) {
        String texto = descripcion.trim();
        if (texto.length() < 3 || texto.chars().noneMatch(Character::isLetter)) {
            return null;
        }

        Producto producto = emparejarProducto(texto, catalogo);
        return new LineaExtraccion(
                texto,
                cantidad,
                unidad == null ? null : unidad.toLowerCase(Locale.ROOT),
                precio,
                importe,
                producto != null ? producto.getId() : null,
                null,
                null);
    }

    private Producto emparejarProducto(String descripcion, List<Producto> catalogo) {
        String buscado = normalizar(descripcion);

        return catalogo.stream()
                .filter(producto -> buscado.contains(normalizar(producto.getNombre())))
                .findFirst()
                .or(
                        () ->
                                catalogo.stream()
                                        .filter(
                                                producto ->
                                                        coincidenciaParcial(
                                                                buscado,
                                                                normalizar(producto.getNombre())))
                                        .findFirst())
                .orElse(null);
    }

    private boolean esRuido(String linea) {
        String normalizada = normalizar(linea);
        return normalizada.length() < 6
                || normalizada.contains("total")
                || normalizada.contains("base imponible")
                || normalizada.contains("subtotal")
                || normalizada.contains("iva")
                || normalizada.contains("cif")
                || normalizada.contains("telefono")
                || normalizada.contains("factura")
                || normalizada.contains("albaran")
                || normalizada.contains("fecha")
                || normalizada.contains("descripcion cantidad");
    }

    private boolean cuadra(BigDecimal cantidad, BigDecimal precio, BigDecimal importe) {
        BigDecimal calculado = cantidad.multiply(precio).setScale(2, RoundingMode.HALF_UP);
        return calculado.subtract(importe).abs().compareTo(new BigDecimal("0.15")) <= 0;
    }

    private BigDecimal buscarImporte(List<String> lineas, String... etiquetas) {
        BigDecimal encontrado = null;
        for (String linea : lineas) {
            String normalizada = normalizar(linea);
            boolean coincide =
                    Arrays.stream(etiquetas)
                            .map(ParserAlbaran::normalizar)
                            .anyMatch(normalizada::contains);
            if (!coincide || (normalizada.contains("subtotal") && etiquetas[0].equals("TOTAL"))) {
                continue;
            }
            BigDecimal ultimo = ultimoImporte(linea);
            if (ultimo != null) {
                encontrado = ultimo;
            }
        }
        return encontrado;
    }

    private BigDecimal ultimoImporte(String linea) {
        Matcher matcher = IMPORTE.matcher(linea);
        BigDecimal ultimo = null;
        while (matcher.find()) {
            ultimo = numero(matcher.group(1));
        }
        return ultimo;
    }

    private String nombreProveedorProbable(List<String> lineas) {
        return lineas.stream()
                .filter(linea -> linea.chars().anyMatch(Character::isLetter))
                .findFirst()
                .map(String::trim)
                .orElse(null);
    }

    private boolean coincidenciaParcial(String texto, String objetivo) {
        List<String> palabras =
                Arrays.stream(objetivo.split("\\s+"))
                        .filter(palabra -> !PALABRAS_IGNORADAS.contains(palabra))
                        .toList();
        if (palabras.isEmpty()) {
            return false;
        }
        long encontradas = palabras.stream().filter(texto::contains).count();
        return encontradas * 2 >= palabras.size();
    }

    static BigDecimal numero(String valor) {
        String limpio = valor.trim().replace(" ", "");
        if (limpio.contains(",")) {
            limpio = limpio.replace(".", "").replace(",", ".");
        }
        try {
            return new BigDecimal(limpio).setScale(3, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos =
                Normalizer.normalize(valor, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }
}
