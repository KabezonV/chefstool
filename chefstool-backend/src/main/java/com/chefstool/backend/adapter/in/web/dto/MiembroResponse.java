package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.MiembroOrganizacion;
import java.util.UUID;

public record MiembroResponse(UUID id, UUID usuarioId, String nombre, String email, String rol) {

    public static MiembroResponse from(MiembroOrganizacion miembro) {
        return new MiembroResponse(
                miembro.getId(),
                miembro.getUsuarioId(),
                miembro.getNombre(),
                miembro.getEmail(),
                miembro.getRol());
    }
}
