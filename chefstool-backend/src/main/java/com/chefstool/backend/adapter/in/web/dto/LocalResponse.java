package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Local;
import java.util.UUID;

public record LocalResponse(UUID id, String nombre, boolean activo) {
    public static LocalResponse from(Local local) {
        return new LocalResponse(local.getId(), local.getNombre(), local.isActivo());
    }
}
