package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record LocalRequest(@NotBlank String nombre) {}
