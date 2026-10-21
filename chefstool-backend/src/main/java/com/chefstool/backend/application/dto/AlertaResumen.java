package com.chefstool.backend.application.dto;

import com.chefstool.backend.domain.model.AlertaPrecio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertaResumen(
        UUID id,
        String mensaje,
        String severidad,
        BigDecimal porcentajeCambio,
        String productoNombre,
        String proveedorNombre,
        OffsetDateTime createdAt) {

    public static AlertaResumen from(AlertaPrecio alerta) {
        return new AlertaResumen(
                alerta.getId(),
                alerta.getMensaje(),
                alerta.getSeveridad(),
                alerta.getPorcentajeCambio(),
                alerta.getProducto() != null ? alerta.getProducto().getNombre() : null,
                alerta.getProveedor() != null ? alerta.getProveedor().getNombre() : null,
                alerta.getCreatedAt());
    }
}
