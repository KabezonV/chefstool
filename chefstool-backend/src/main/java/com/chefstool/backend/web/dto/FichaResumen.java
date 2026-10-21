package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FichaResumen(
        UUID id,
        String nombre,
        Integer raciones,
        BigDecimal precioVenta,
        BigDecimal costePorRacion,
        BigDecimal margenPct,
        int numeroLineas) {}
