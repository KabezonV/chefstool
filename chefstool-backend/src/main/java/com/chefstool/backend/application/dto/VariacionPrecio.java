package com.chefstool.backend.application.dto;

import java.math.BigDecimal;

public record VariacionPrecio(
        String producto,
        String proveedor,
        BigDecimal porcentaje,
        BigDecimal precioAnterior,
        BigDecimal precioNuevo) {}
