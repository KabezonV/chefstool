package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Invitacion;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvitacionResponse(
        UUID id,
        String email,
        String rol,
        String estado,
        OffsetDateTime createdAt) {

    public static InvitacionResponse pendiente(Invitacion invitacion) {
        return new InvitacionResponse(
                invitacion.getId(),
                invitacion.getEmail(),
                invitacion.getRol(),
                "pendiente",
                invitacion.getCreatedAt());
    }

    public static InvitacionResponse anadido(MiembroResponse miembro) {
        return new InvitacionResponse(
                miembro.id(), miembro.email(), miembro.rol(), "anadido", null);
    }
}
