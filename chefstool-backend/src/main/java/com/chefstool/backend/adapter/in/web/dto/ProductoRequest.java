package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 80) String categoria,
        @Size(max = 20) String unidadBase) {}
