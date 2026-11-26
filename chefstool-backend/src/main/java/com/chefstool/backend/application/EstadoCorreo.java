package com.chefstool.backend.application;

import java.util.List;

/**
 * Foto del estado del correo saliente: sirve para saber si faltan datos antes de prometer
 * que las alertas van a llegar.
 */
public record EstadoCorreo(
        boolean disponible,
        boolean alertasActivas,
        String host,
        String remitente,
        List<String> falta) {}
