package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record FichaRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 500) String descripcion,
        @Positive Integer raciones,
        BigDecimal margenObjetivo,
        BigDecimal precioVenta,
        List<FichaLineaRequest> lineas) {}
