package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record EventoRequest(
        @NotBlank @Size(max = 120) String nombre,
        LocalDate fecha,
        @Positive Integer comensales,
        UUID clienteId,
        @Size(max = 500) String notas) {}
