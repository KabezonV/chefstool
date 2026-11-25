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

        List<LineaExtraccion> lineas = combinarLineas(vision, local);
        BigDecimal sumaLocal = lineas.stream()
                .map(LineaExtraccion::importe)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal neto = local.importeNeto() != null && local.importeNeto().signum() > 0
                ? local.importeNeto()
                : (vision.importeNeto() != null ? vision.importeNeto() : sumaLocal);
        BigDecimal iva = local.importeIva() != null && local.importeIva().signum() > 0
                ? local.importeIva()
                : (vision.importeIva() == null ? BigDecimal.ZERO : vision.importeIva());
        BigDecimal total = local.importeTotal() != null && local.importeTotal().signum() > 0
                ? local.importeTotal()
                : (vision.importeTotal() == null ? neto.add(iva) : vision.importeTotal());

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

        BigDecimal confianza = lecturaVision.confianza();
        if (lecturaLocal.confianza().compareTo(confianza) > 0) {
            confianza = lecturaLocal.confianza();
        }
        if (!lineas.isEmpty()) {
            confianza = confianza.max(new BigDecimal("0.85"));
        }
        return new ResultadoExtraccion(combinada, confianza);
    }

    private List<LineaExtraccion> combinarLineas(
            ExtraccionDocumento vision, ExtraccionDocumento local) {
        List<LineaExtraccion> locales =
                local.lineas() == null ? List.of() : local.lineas();
        if (locales.isEmpty()) {
            return vision.lineas() == null ? List.of() : vision.lineas();
        }

        List<LineaExtraccion> resultado = new ArrayList<>(locales);
        List<LineaExtraccion> deVision =
                vision.lineas() == null ? List.of() : vision.lineas();
        for (LineaExtraccion lineaVision : deVision) {
            boolean yaIncluida = resultado.stream().anyMatch(
                    lineaLocal -> descripcionCoincide(lineaLocal.descripcion(), lineaVision.descripcion()));
            if (!yaIncluida) {
                resultado.add(lineaVision);
            }
        }
        return resultado;
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
