package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Organizacion;
import java.util.UUID;

public record OrganizacionResponse(UUID id, String nombre) {

    public static OrganizacionResponse from(Organizacion organizacion) {
        return new OrganizacionResponse(organizacion.getId(), organizacion.getNombre());
    }
}
