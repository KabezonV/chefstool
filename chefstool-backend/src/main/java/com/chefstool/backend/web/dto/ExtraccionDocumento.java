package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExtraccionDocumento(
        String proveedorNombre,
        UUID proveedorId,
        String numeroDocumento,
        LocalDate fecha,
        String moneda,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeTotal,
        List<LineaExtraccion> lineas) {}
