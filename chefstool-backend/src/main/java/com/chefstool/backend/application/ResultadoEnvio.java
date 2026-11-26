package com.chefstool.backend.application;

/** Resultado de intentar enviar un correo de prueba. */
public record ResultadoEnvio(boolean enviado, String destinatario, String mensaje) {}
