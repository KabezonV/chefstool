package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentoDetalle(
        UUID id,
        String nombreOriginal,
        String estado,
        String origen,
        BigDecimal confianzaExtraccion,
        OffsetDateTime createdAt,
        boolean tieneArchivo,
        ExtraccionDocumento extraccion) {}
