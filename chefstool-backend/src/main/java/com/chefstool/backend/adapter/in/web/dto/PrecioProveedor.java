package com.chefstool.backend.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PrecioProveedor(
        UUID proveedorId,
        String proveedorNombre,
        BigDecimal precioActual,
        BigDecimal precioMinimo,
        BigDecimal precioMaximo,
        LocalDate ultimaCompra) {}
