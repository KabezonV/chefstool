package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 20) String cif,
        @Email @Size(max = 120) String email,
        @Size(max = 30) String telefono,
        @Size(max = 500) String notas) {}
