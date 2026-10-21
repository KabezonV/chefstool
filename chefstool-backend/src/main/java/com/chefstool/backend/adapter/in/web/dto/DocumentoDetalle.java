package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.ExtraccionDocumento;

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
