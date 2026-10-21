package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizacionRequest(@NotBlank @Size(max = 120) String nombre) {}
