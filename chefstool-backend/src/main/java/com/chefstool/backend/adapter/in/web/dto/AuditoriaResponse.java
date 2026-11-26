package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Auditoria;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditoriaResponse(
        UUID id,
        UUID usuarioId,
        String usuarioNombre,
        String accion,
        String entidad,
        UUID entidadId,
        String detalle,
        OffsetDateTime createdAt) {
    public static AuditoriaResponse from(Auditoria auditoria) {
        return from(auditoria, null);
    }

    public static AuditoriaResponse from(Auditoria auditoria, String usuarioNombre) {
        return new AuditoriaResponse(
                auditoria.getId(),
                auditoria.getUsuarioId(),
                usuarioNombre,
                auditoria.getAccion(),
                auditoria.getEntidad(),
                auditoria.getEntidadId(),
                auditoria.getDetalle(),
                auditoria.getCreatedAt());
    }
}
