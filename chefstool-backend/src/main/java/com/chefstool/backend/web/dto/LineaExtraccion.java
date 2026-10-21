package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Linea tal y como se revisa en pantalla antes de confirmar la compra. */
public record LineaExtraccion(
        String descripcion,
        BigDecimal cantidad,
        String unidad,
        BigDecimal precioUnitario,
        BigDecimal importe,
        UUID productoId,
        UUID clienteId,
        UUID eventoId) {}
