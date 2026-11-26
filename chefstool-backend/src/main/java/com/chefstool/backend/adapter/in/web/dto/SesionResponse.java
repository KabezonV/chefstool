package com.chefstool.backend.adapter.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record SesionResponse(
        String token,
        String refreshToken,
        Instant expiresAt,
        UUID usuarioId,
        String email,
        String nombre,
        UUID organizacionId,
        String organizacion,
        UUID localId,
        String rol) {

    public SesionResponse(
            String token,
            UUID usuarioId,
            String email,
            String nombre,
            UUID organizacionId,
            String organizacion,
            String rol) {
        this(token, null, null, usuarioId, email, nombre, organizacionId, organizacion, null, rol);
    }
}
