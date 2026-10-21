package com.chefstool.backend.adapter.in.web.dto;

import java.util.UUID;

public record SesionResponse(
        String token,
        UUID usuarioId,
        String email,
        String nombre,
        UUID organizacionId,
        String organizacion,
        String rol) {}
