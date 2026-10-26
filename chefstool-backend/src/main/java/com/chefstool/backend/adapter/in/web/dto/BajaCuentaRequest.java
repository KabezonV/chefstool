package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BajaCuentaRequest(@NotBlank @Size(max = 100) String password) {}
