package com.chefstool.backend.web.dto;

import com.chefstool.backend.domain.LineaCompra;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaCompraResponse(
        UUID id,
        String descripcion,
        BigDecimal cantidad,
        String unidad,
        BigDecimal precioUnitario,
        BigDecimal importe,
        UUID productoId,
        String productoNombre) {

    public static LineaCompraResponse from(LineaCompra linea) {
        return new LineaCompraResponse(
                linea.getId(),
                linea.getDescripcionOriginal(),
                linea.getCantidad(),
                linea.getUnidad(),
                linea.getPrecioUnitario(),
                linea.getImporte(),
                linea.getProducto() != null ? linea.getProducto().getId() : null,
                linea.getProducto() != null ? linea.getProducto().getNombre() : null);
    }
}
