package com.chefstool.backend.web.dto;

import com.chefstool.backend.domain.Compra;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CompraResponse(
        UUID id,
        String proveedorNombre,
        String numeroDocumento,
        LocalDate fecha,
        String moneda,
        BigDecimal importeTotal,
        int numeroLineas) {

    public static CompraResponse from(Compra compra, int numeroLineas) {
        return new CompraResponse(
                compra.getId(),
                compra.getProveedor() != null ? compra.getProveedor().getNombre() : null,
                compra.getNumeroDocumento(),
                compra.getFecha(),
                compra.getMoneda(),
                compra.getImporteTotal(),
                numeroLineas);
    }
}
