package com.chefstool.backend.adapter.in.web.dto;

/**
 * {@code token} solo viaja cuando el backend esta en modo demostracion
 * ({@code chefstool.auth.enlace-directo=true}); en produccion se envia por correo.
 */
public record RecuperacionResponse(String mensaje, String token) {}
