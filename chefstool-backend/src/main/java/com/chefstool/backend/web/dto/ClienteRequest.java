package com.chefstool.backend.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Size(max = 120) String nombre,
        @Email @Size(max = 120) String email,
        @Size(max = 30) String telefono,
        @Size(max = 500) String notas) {}
