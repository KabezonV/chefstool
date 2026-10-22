package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InvitacionRequest(
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 20) String rol) {}
