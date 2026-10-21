package com.chefstool.backend.web.dto;

import com.chefstool.backend.domain.Compra;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CompraDetalle(
        UUID id,
        UUID proveedorId,
        String proveedorNombre,
        String numeroDocumento,
        LocalDate fecha,
        String moneda,
        BigDecimal importeNeto,
        BigDecimal importeIva,
        BigDecimal importeTotal,
        UUID documentoId,
        List<LineaCompraResponse> lineas) {

    public static CompraDetalle from(Compra compra, List<LineaCompraResponse> lineas) {
        return new CompraDetalle(
                compra.getId(),
                compra.getProveedor() != null ? compra.getProveedor().getId() : null,
                compra.getProveedor() != null ? compra.getProveedor().getNombre() : null,
                compra.getNumeroDocumento(),
                compra.getFecha(),
                compra.getMoneda(),
                compra.getImporteNeto(),
                compra.getImporteIva(),
                compra.getImporteTotal(),
                compra.getDocumento() != null ? compra.getDocumento().getId() : null,
                lineas);
    }
}
