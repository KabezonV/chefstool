package com.chefstool.backend.adapter.out.ocr;

import com.chefstool.backend.domain.model.ResultadoExtraccion;
import com.chefstool.backend.domain.port.out.ExtractorDocumento;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
                return vision.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
            } catch (RuntimeException ex) {
                log.warn("Lectura con IA no disponible para {}: {}", nombreOriginal, ex.getMessage());
            }
        }
        return local.extraer(contenido, nombreOriginal, tipoMime, organizacionId);
    }
}
