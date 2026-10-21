package com.chefstool.backend.web.dto;

import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;

public record FichaLineaRequest(
        UUID productoId,
        String descripcion,
        @PositiveOrZero BigDecimal cantidad,
        String unidad) {}
