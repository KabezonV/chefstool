package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.EstadoSolicitudCompra;
import com.chefstool.backend.domain.model.SolicitudCompra;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SolicitudCompraResponse(
        UUID id,
        UUID localId,
        String concepto,
        String descripcion,
        String proveedor,
        BigDecimal importe,
        EstadoSolicitudCompra estado,
        UUID solicitanteId,
        String solicitanteNombre,
        UUID revisadaPor,
        String revisadaPorNombre,
        OffsetDateTime revisadaAt,
        String motivoRechazo,
        OffsetDateTime createdAt) {

    public static SolicitudCompraResponse from(
            SolicitudCompra solicitud, String solicitanteNombre, String revisadaPorNombre) {
        return new SolicitudCompraResponse(
                solicitud.getId(),
                solicitud.getLocalId(),
                solicitud.getConcepto(),
                solicitud.getDescripcion(),
                solicitud.getProveedor(),
                solicitud.getImporte(),
                solicitud.getEstado(),
                solicitud.getSolicitanteId(),
                solicitanteNombre,
                solicitud.getRevisadaPor(),
                revisadaPorNombre,
                solicitud.getRevisadaAt(),
                solicitud.getMotivoRechazo(),
                solicitud.getCreatedAt());
    }
}
