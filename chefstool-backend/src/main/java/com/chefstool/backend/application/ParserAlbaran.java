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
                    "(?:N[º°o]?\\.?|NUM(?:ERO)?|FACTURA|ALBAR[AÁ]N)\\s*[:.]?\\s*"
                            + "((?=[A-Z0-9\\-/]*\\d)[A-Z0-9][A-Z0-9\\-/]{2,})",
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

    /** Formas juridicas que no identifican a un proveedor concreto. */
    private static final Set<String> FORMAS_JURIDICAS =
            Set.of("sl", "slu", "sau", "sa", "cb", "scoop", "sociedad", "slne", "scp", "coop");

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
                .or(
                        () ->
                                proveedores.stream()
                                        .filter(
                                                proveedor ->
                                                        mencionaNombreCompleto(
                                                                primerasLineas,
                                                                normalizar(proveedor.getNombre())))
                                        .findFirst());
    }

    /**
     * Exige todas las palabras significativas del nombre. Antes bastaba con compartir la mitad,
     * y un albaran de "DISTRIBUCIONES HORTOFRUTICOLAS EJEMPLO" se asignaba a cualquier proveedor
     * del catalogo que se llamara "Distribuciones ...".
     */
    private boolean mencionaNombreCompleto(String texto, String objetivo) {
        List<String> palabrasTexto = Arrays.asList(texto.split("\\s+"));
        List<String> palabras = palabrasSignificativas(objetivo);
        return !palabras.isEmpty() && palabrasTexto.containsAll(palabras);
    }

    private static List<String> palabrasSignificativas(String nombre) {
        return Arrays.stream(nombre.split("\\s+"))
                .filter(palabra -> palabra.length() >= 3)
                .filter(palabra -> !PALABRAS_IGNORADAS.contains(palabra))
                .filter(palabra -> !FORMAS_JURIDICAS.contains(palabra))
                .toList();
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
        if (!matcher.find()) {
            return null;
        }
        // Un numero cortado por el OCR ("A-2026/") no debe quedarse con el separador final
        String numero = matcher.group(1);
        int fin = numero.length();
        while (fin > 0 && (numero.charAt(fin - 1) == '-' || numero.charAt(fin - 1) == '/')) {
            fin--;
        }
        return numero.substring(0, fin);
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

            // La linea ya tiene la forma completa: si las cuentas no salen, es ruido.
            return cuadra(cantidad, precio, importe)
                    ? construir(
                            conUnidad.group(1),
                            cantidad,
                            conUnidad.group(3),
                            precio,
                            importe,
                            catalogo)
                    : null;
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

    /** El OCR a veces pega la cantidad al nombre: "Tomate pera —10kg". */
    private static final Pattern CANTIDAD_PEGADA =
            Pattern.compile(
                    "\\s*[—–-]?\\s*\\d{1,4}(?:[.,]\\d{1,3})?\\s*(kg|g|l|ml|ud|uds|un|doc|caja|bolsa)\\b\\s*$",
                    Pattern.CASE_INSENSITIVE);

    private LineaExtraccion construir(
            String descripcion,
            BigDecimal cantidad,
            String unidad,
            BigDecimal precio,
            BigDecimal importe,
            List<Producto> catalogo) {
        String original = descripcion.trim();
        String unidadFinal = unidad;
        Matcher pegado = CANTIDAD_PEGADA.matcher(original);
        String texto = original;
        if (pegado.find()) {
            texto = original.substring(0, pegado.start()).trim();
            if (unidadFinal == null || unidadFinal.isBlank()) {
                unidadFinal = pegado.group(1);
            }
        }
        if (texto.length() < 3 || texto.chars().noneMatch(Character::isLetter)) {
            return null;
        }

        Producto producto = emparejarProducto(texto, catalogo);
        return new LineaExtraccion(
                texto,
                cantidad,
                unidadFinal == null ? null : unidadFinal.toLowerCase(Locale.ROOT),
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

    /**
     * Etiquetas de cabecera y pie que no son lineas de articulo. Se busca la palabra completa:
     * "IVA" suelto descarta la linea, pero "aceite de oliva" no.
     */
    private static final Pattern RUIDO =
            Pattern.compile(
                    "\\b(total|subtotal|iva|cif|factura|albaran|fecha|telefono)\\b"
                            + "|base imponible|descripcion cantidad");

    private boolean esRuido(String linea) {
        String normalizada = normalizar(linea);
        return normalizada.length() < 6 || RUIDO.matcher(normalizada).find();
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

    /** Formas juridicas tipicas: la razon social suele estar en la cabecera. */
    private static final Pattern RAZON_SOCIAL =
            Pattern.compile("\\b(S\\.?L\\.?U?|S\\.?A\\.?|C\\.?B\\.?|SCOOP|SOCIEDAD)\\b",
                    Pattern.CASE_INSENSITIVE);

    private String nombreProveedorProbable(List<String> lineas) {
        // 1) Una linea con forma de razon social ("Muebles Salinas Rodriguez, S.L.")
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (RAZON_SOCIAL.matcher(limpia).find() && contieneTexto(limpia)) {
                return limpia;
            }
        }
        // 2) La primera linea con letras y sin numeros (los importes y fechas no valen)
        for (String linea : lineas) {
            String limpia = linea.trim();
            if (contieneTexto(limpia)
                    && limpia.chars().noneMatch(Character::isDigit)
                    && limpia.length() >= 3
                    && limpia.length() <= 60) {
                return limpia;
            }
        }
        return lineas.stream()
                .filter(linea -> linea.chars().anyMatch(Character::isLetter))
                .findFirst()
                .map(String::trim)
                .orElse(null);
    }

    private boolean contieneTexto(String linea) {
        return linea != null && linea.chars().filter(Character::isLetter).count() >= 3;
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
