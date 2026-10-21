package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.DocumentoCompra;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentoResumen(
        UUID id,
        String nombreOriginal,
        String estado,
        String origen,
        BigDecimal confianzaExtraccion,
        OffsetDateTime createdAt) {

    public static DocumentoResumen from(DocumentoCompra documento) {
        return new DocumentoResumen(
                documento.getId(),
                documento.getNombreOriginal(),
                documento.getEstado(),
                documento.getOrigen(),
                documento.getConfianzaExtraccion(),
                documento.getCreatedAt());
    }
}
