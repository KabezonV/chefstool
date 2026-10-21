package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Compra;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompraResumen(
        UUID id,
        String proveedorNombre,
        String numeroDocumento,
        LocalDate fecha,
        String moneda,
        BigDecimal importeTotal,
        UUID documentoId) {

    public static CompraResumen from(Compra compra) {
        return new CompraResumen(
                compra.getId(),
                compra.getProveedor() != null ? compra.getProveedor().getNombre() : null,
                compra.getNumeroDocumento(),
                compra.getFecha(),
                compra.getMoneda(),
                compra.getImporteTotal(),
                compra.getDocumento() != null ? compra.getDocumento().getId() : null);
    }
}
