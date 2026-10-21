package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FichaDetalle(
        UUID id,
        String nombre,
        String descripcion,
        Integer raciones,
        BigDecimal margenObjetivo,
        BigDecimal precioVenta,
        BigDecimal costeTotal,
        BigDecimal costePorRacion,
        BigDecimal margenPct,
        int productosSinPrecio,
        List<FichaLineaResponse> lineas) {}
