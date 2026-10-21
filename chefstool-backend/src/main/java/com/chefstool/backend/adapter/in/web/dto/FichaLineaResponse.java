package com.chefstool.backend.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FichaLineaResponse(
        UUID id,
        UUID productoId,
        String productoNombre,
        String descripcion,
        BigDecimal cantidad,
        String unidad,
        BigDecimal precioUnitario,
        BigDecimal coste) {}
