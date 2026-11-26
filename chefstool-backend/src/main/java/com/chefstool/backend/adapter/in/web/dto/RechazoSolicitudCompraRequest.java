package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazoSolicitudCompraRequest(
        @NotBlank @Size(max = 1000) String motivo) {}
