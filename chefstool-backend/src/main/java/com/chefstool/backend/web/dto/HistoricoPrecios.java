package com.chefstool.backend.web.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record HistoricoPrecios(
        UUID productoId,
        String nombre,
        String unidadBase,
        String mejorProveedor,
        BigDecimal mejorPrecio,
        List<PrecioProveedor> proveedores,
        List<PuntoPrecio> evolucion) {}
