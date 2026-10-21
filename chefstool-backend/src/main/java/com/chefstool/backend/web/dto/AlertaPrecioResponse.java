package com.chefstool.backend.web.dto;

import com.chefstool.backend.domain.AlertaPrecio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertaPrecioResponse(
        UUID id,
        String mensaje,
        String severidad,
        BigDecimal porcentajeCambio,
        BigDecimal precioAnterior,
        BigDecimal precioNuevo,
        String productoNombre,
        String proveedorNombre,
        boolean reconocida,
        OffsetDateTime createdAt) {

    public static AlertaPrecioResponse from(AlertaPrecio alerta) {
        return new AlertaPrecioResponse(
                alerta.getId(),
                alerta.getMensaje(),
                alerta.getSeveridad(),
                alerta.getPorcentajeCambio(),
                alerta.getPrecioAnterior(),
                alerta.getPrecioNuevo(),
                alerta.getProducto() != null ? alerta.getProducto().getNombre() : null,
                alerta.getProveedor() != null ? alerta.getProveedor().getNombre() : null,
                alerta.isReconocida(),
                alerta.getCreatedAt());
    }
}
