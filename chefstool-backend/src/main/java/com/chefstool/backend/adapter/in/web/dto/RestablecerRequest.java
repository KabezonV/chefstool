package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerRequest(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 8, max = 100) String password) {}
