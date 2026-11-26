package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.domain.model.ExtraccionDocumento;
import com.chefstool.backend.domain.model.LineaExtraccion;
import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Usa el modelo de vision si esta configurado y, si falla (sin cuota, sin red, respuesta
 * rara), cae al lector local con Tesseract para no dejar al usuario sin nada.
 */
@Service
@Primary
public class ExtractorHibrido implements ExtractorDocumento {

    private static final Logger log = LoggerFactory.getLogger(ExtractorHibrido.class);

    private final ExtractorVision vision;
    private final ExtractorTexto local;

    public ExtractorHibrido(ExtractorVision vision, ExtractorTexto local) {
        this.vision = vision;
        this.local = local;
    }

    @Override
    public ResultadoExtraccion extraer(
            byte[] contenido, String nombreOriginal, String tipoMime, UUID organizacionId) {
        if (vision.disponible() && contenido != null && contenido.length > 0) {
            try {
                ResultadoExtraccion lecturaVision =
                        vision.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
                try {
                    ResultadoExtraccion lecturaLocal =
                            local.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
                    return combinar(lecturaVision, lecturaLocal);
                } catch (RuntimeException localException) {
                    log.debug(
                            "No se pudo contrastar la lectura IA con Tesseract para {}: {}",
                            nombreOriginal,
                            localException.getMessage());
                    return lecturaVision;
                }
            } catch (RuntimeException ex) {
                log.warn("Lectura con IA no disponible para {}: {}", nombreOriginal, ex.getMessage());
            }
        }
        return local.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
    }

    /** Diferencia admitida entre la suma de lineas, la base y el total (redondeos de 2-3 lineas). */
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.10");

    /** Tope de confianza cuando los importes leidos no cuadran entre si. */
    private static final BigDecimal CONFIANZA_DUDOSA = new BigDecimal("0.60");

    /** Confianza minima cuando la lectura cuadra y trae lineas contrastadas por dos lectores. */
    private static final BigDecimal CONFIANZA_CONTRASTADA = new BigDecimal("0.85");

    /** Contrasta los datos de la IA con Tesseract y conserva el dato local más verificable. */
    ResultadoExtraccion combinar(
            ResultadoExtraccion lecturaVision, ResultadoExtraccion lecturaLocal) {
        ExtraccionDocumento vision = lecturaVision.extraccion();
        ExtraccionDocumento local = lecturaLocal.extraccion();

        if (vision == local) {
            return lecturaVision;
        }
        if (local == null) {
            return lecturaVision;
        }

        // Tesseract lee el texto con mas fidelidad, pero en fotos con sombra o torcidas se
        // equivoca mas que la IA al componer los importes. Se parte de la lectura que cuadra
        // mejor y, si empatan, de la de la IA, que transcribe mejor las descripciones.
        boolean usarLocal = desvio(local) < desvio(vision);
        ExtraccionDocumento referencia = usarLocal ? local : vision;
        ExtraccionDocumento alternativa = usarLocal ? vision : local;

        List<LineaExtraccion> lineas = combinarLineas(vision, local, usarLocal);
        BigDecimal sumaLocal =
                lineas.stream()
                        .map(LineaExtraccion::importe)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal neto = importePositivo(referencia.importeNeto(), alternativa.importeNeto(), sumaLocal);
        BigDecimal iva = importeConCero(referencia.importeIva(), alternativa.importeIva());
        BigDecimal total = importePositivo(
                referencia.importeTotal(),
                alternativa.importeTotal(),
                neto.add(iva));

        ExtraccionDocumento combinada =
                new ExtraccionDocumento(
                        primero(local.proveedorNombre(), vision.proveedorNombre()),
                        primeroId(local.proveedorId(), vision.proveedorId()),
                        primero(local.numeroDocumento(), vision.numeroDocumento()),
                        primeroFecha(local.fecha(), vision.fecha()),
                        vision.moneda() == null || vision.moneda().isBlank()
                                ? (local.moneda() == null ? "EUR" : local.moneda())
                                : vision.moneda(),
                        neto.setScale(2, RoundingMode.HALF_UP),
                        iva.setScale(2, RoundingMode.HALF_UP),
                        total.setScale(2, RoundingMode.HALF_UP),
                        lineas);

        // Antes cualquier lectura con lineas se mostraba como "alta confianza" (0,85), aunque el
        // total no cuadrase con la suma de lineas. Eso empujaba a confirmar importes erroneos.
        BigDecimal confianza = lecturaVision.confianza().max(lecturaLocal.confianza());
        boolean discrepan = discrepanImportes(vision.importeTotal(), local.importeTotal());
        log.debug(
                "Contraste OCR: vision total={} neto={} | tesseract total={} neto={} | cuadra={} discrepan={}",
                vision.importeTotal(),
                vision.importeNeto(),
                local.importeTotal(),
                local.importeNeto(),
                cuadra(combinada),
                discrepan);
        if (!cuadra(combinada) || discrepan) {
            confianza = confianza.min(CONFIANZA_DUDOSA);
        } else if (!lineas.isEmpty()) {
            confianza = confianza.max(CONFIANZA_CONTRASTADA);
        }
        return new ResultadoExtraccion(combinada, confianza);
    }

    /** Los dos lectores dan importes distintos: alguien se ha equivocado al copiar una cifra. */
    private boolean discrepanImportes(BigDecimal uno, BigDecimal otro) {
        if (uno == null || otro == null || uno.signum() <= 0 || otro.signum() <= 0) {
            return false;
        }
        return uno.subtract(otro).abs().compareTo(TOLERANCIA) > 0;
    }

    /** Suma de las lineas frente a la base y base mas IVA frente al total. */
    boolean cuadra(ExtraccionDocumento extraccion) {
        if (extraccion == null || extraccion.lineas() == null || extraccion.lineas().isEmpty()) {
            return false;
        }
        BigDecimal neto = extraccion.importeNeto();
        if (neto == null || neto.signum() <= 0) {
            return false;
        }
        BigDecimal suma = extraccion.lineas().stream()
                .map(LineaExtraccion::importe)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.subtract(neto).abs().compareTo(TOLERANCIA) > 0) {
            return false;
        }
        BigDecimal total = extraccion.importeTotal();
        if (total != null && total.signum() > 0) {
            BigDecimal iva =
                    extraccion.importeIva() == null ? BigDecimal.ZERO : extraccion.importeIva();
            return neto.add(iva).subtract(total).abs().compareTo(TOLERANCIA) <= 0;
        }
        return true;
    }

    /** Cuanto se aleja la lectura de cuadrar: 0 es una lectura perfecta. */
    private double desvio(ExtraccionDocumento extraccion) {
        if (extraccion == null || extraccion.lineas() == null || extraccion.lineas().isEmpty()) {
            return Double.MAX_VALUE;
        }
        BigDecimal neto = extraccion.importeNeto();
        if (neto == null || neto.signum() <= 0) {
            return Double.MAX_VALUE / 2;
        }
        BigDecimal suma = extraccion.lineas().stream()
                .map(LineaExtraccion::importe)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        double desvioLineas = suma.subtract(neto).abs().doubleValue();
        BigDecimal total = extraccion.importeTotal();
        if (total != null && total.signum() > 0) {
            BigDecimal iva =
                    extraccion.importeIva() == null ? BigDecimal.ZERO : extraccion.importeIva();
            desvioLineas += neto.add(iva).subtract(total).abs().doubleValue();
        }
        return desvioLineas;
    }

    private BigDecimal importePositivo(BigDecimal primero, BigDecimal segundo, BigDecimal ultimo) {
        if (primero != null && primero.signum() > 0) {
            return primero;
        }
        if (segundo != null && segundo.signum() > 0) {
            return segundo;
        }
        return ultimo == null ? BigDecimal.ZERO : ultimo;
    }

    private BigDecimal importeConCero(BigDecimal primero, BigDecimal segundo) {
        if (primero != null && primero.signum() > 0) {
            return primero;
        }
        return segundo == null ? BigDecimal.ZERO : segundo;
    }

    private List<LineaExtraccion> combinarLineas(
            ExtraccionDocumento vision, ExtraccionDocumento local, boolean usarLocal) {
        List<LineaExtraccion> base = usarLocal ? local.lineas() : vision.lineas();
        List<LineaExtraccion> extra = usarLocal ? vision.lineas() : local.lineas();

        if (base == null || base.isEmpty()) {
            return extra == null ? List.of() : extra;
        }

        // Se parte de la lectura que cuadra mejor: en fotos con sombra o giradas Tesseract
        // devuelve cantidades inventadas (3.167 kg) y la IA no. Antes se partia siempre de la
        // local, y esas lineas rotas acababan en la compra.
        List<LineaExtraccion> resultado = new ArrayList<>(base);
        if (extra != null) {
            for (LineaExtraccion linea : extra) {
                boolean yaIncluida =
                        resultado.stream().anyMatch(existente -> mismaLinea(existente, linea));
                if (!yaIncluida) {
                    resultado.add(linea);
                }
            }
        }
        return resultado;
    }

    /**
     * Dos lectores describen la misma linea del albaran. Ademas del texto igual o contenido,
     * se aceptan descripciones parecidas ("LE CHUGA ROMANA" y "LECHUGA ROMANA") y, cuando los
     * numeros coinciden, descripciones claramente rotas por el OCR ("&lt;ANAHOR1a").
     */
    private boolean mismaLinea(LineaExtraccion uno, LineaExtraccion otro) {
        if (descripcionCoincide(uno.descripcion(), otro.descripcion())) {
            return true;
        }
        if (descripcionParecida(uno.descripcion(), otro.descripcion())) {
            return true;
        }
        return numerosIguales(uno, otro)
                && (descripcionDudosa(uno.descripcion()) || descripcionDudosa(otro.descripcion()));
    }

    /** Palabras compartidas sobre el total: "LE CHUGA ROMANA" y "LECHUGA ROMANA" dan 0,5. */
    private boolean descripcionParecida(String izquierda, String derecha) {
        List<String> a = palabras(normalizarDescripcion(izquierda));
        List<String> b = palabras(normalizarDescripcion(derecha));
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        long coincidencias = 0;
        List<String> usadas = new ArrayList<>(b);
        for (String palabra : a) {
            for (int i = 0; i < usadas.size(); i++) {
                if (palabraSimilar(palabra, usadas.get(i))) {
                    coincidencias++;
                    usadas.remove(i);
                    break;
                }
            }
        }
        int union = a.size() + b.size() - (int) coincidencias;
        return coincidencias > 0 && union > 0 && (double) coincidencias / union >= 0.5;
    }

    /** Una palabra contiene a la otra ("chuga" en "lechuga") o difiere en una letra mal leida. */
    private boolean palabraSimilar(String una, String otra) {
        if (una.equals(otra)) {
            return true;
        }
        if (una.length() < 4 || otra.length() < 4) {
            return false;
        }
        if (una.contains(otra) || otra.contains(una)) {
            return true;
        }
        // "blanga" y "blanca" son la misma palabra con una letra que el OCR ha bailado
        return una.length() >= 5 && otra.length() >= 5 && distanciaCorta(una, otra) <= 1;
    }

    /** Distancia de edicion, cortada en 2 para que sea barata. */
    private int distanciaCorta(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            int minimoFila = actual[0];
            for (int j = 1; j <= b.length(); j++) {
                int coste = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(
                        Math.min(actual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + coste);
                minimoFila = Math.min(minimoFila, actual[j]);
            }
            if (minimoFila > 1) {
                return 2;
            }
            int[] cambio = anterior;
            anterior = actual;
            actual = cambio;
        }
        return anterior[b.length()];
    }

    private boolean numerosIguales(LineaExtraccion uno, LineaExtraccion otro) {
        return cerca(uno.importe(), otro.importe(), new BigDecimal("0.05"))
                && cerca(uno.cantidad(), otro.cantidad(), new BigDecimal("0.01"))
                && cerca(uno.precioUnitario(), otro.precioUnitario(), new BigDecimal("0.01"));
    }

    private boolean cerca(BigDecimal uno, BigDecimal otro, BigDecimal tolerancia) {
        return uno != null && otro != null && uno.subtract(otro).abs().compareTo(tolerancia) <= 0;
    }

    /** Descripcion con pinta de error de OCR: digitos en medio de una palabra o simbolos. */
    private boolean descripcionDudosa(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return true;
        }
        if (descripcion.matches(".*[<>|_~^\\\\[\\\\]{}].*")) {
            return true;
        }
        for (String palabra : palabras(descripcion)) {
            if (palabra.matches(".*[a-z]\\d+[a-z].*")) {
                return true;
            }
        }
        return false;
    }

    private List<String> palabras(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(texto.split("\\s+"))
                .filter(palabra -> !palabra.isBlank())
                .toList();
    }

    private boolean descripcionCoincide(String izquierda, String derecha) {
        String a = normalizarDescripcion(izquierda);
        String b = normalizarDescripcion(derecha);
        return !a.isBlank() && !b.isBlank() && (a.equals(b) || a.contains(b) || b.contains(a));
    }

    private String normalizarDescripcion(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ").trim();
    }

    private String primero(String primero, String segundo) {
        return primero != null && !primero.isBlank() ? primero : segundo;
    }

    private UUID primeroId(UUID primero, UUID segundo) {
        return primero != null ? primero : segundo;
    }

    private java.time.LocalDate primeroFecha(
            java.time.LocalDate primero, java.time.LocalDate segundo) {
        return primero != null ? primero : segundo;
    }
}
