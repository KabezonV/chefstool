package com.chefstool.backend.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Coste y comportamiento de compra de un proveedor. */
public record ResumenProveedor(
        UUID proveedorId,
        String nombre,
        long compras,
        BigDecimal gastoTotal,
        BigDecimal ticketMedio,
        BigDecimal porcentajeGasto,
        LocalDate ultimaCompra,
        int productos,
        BigDecimal variacionMediaPct) {}
