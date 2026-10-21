package com.chefstool.backend.service.ocr;

import java.util.UUID;

/**
 * Extrae los datos de un albaran o factura.
 *
 * <p>Hoy solo existe la implementacion de demostracion; para conectar un motor real
 * (Mindee, Google Vision, un modelo de vision...) basta con crear otra implementacion
 * marcada como primaria.
 */
public interface ExtractorDocumento {

    ResultadoExtraccion extraer(
            byte[] contenido, String nombreOriginal, String tipoMime, UUID organizacionId);
}
