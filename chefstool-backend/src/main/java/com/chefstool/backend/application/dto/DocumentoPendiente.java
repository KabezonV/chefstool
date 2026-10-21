package com.chefstool.backend.application.dto;

import com.chefstool.backend.domain.model.DocumentoCompra;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentoPendiente(
        UUID id,
        String nombreOriginal,
        String origen,
        BigDecimal confianzaExtraccion,
        OffsetDateTime createdAt) {

    public static DocumentoPendiente from(DocumentoCompra documento) {
        return new DocumentoPendiente(
                documento.getId(),
                documento.getNombreOriginal(),
                documento.getOrigen(),
                documento.getConfianzaExtraccion(),
                documento.getCreatedAt());
    }
}
