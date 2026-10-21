package com.chefstool.backend.service;

import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Resuelve la organizacion activa. Hasta que exista autenticacion (etapa 7)
 * todas las peticiones trabajan sobre la organizacion por defecto creada en la
 * migracion V1.
 */
@Service
public class OrganizacionService {

    private static final UUID ORGANIZACION_POR_DEFECTO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public UUID organizacionActual() {
        return ORGANIZACION_POR_DEFECTO;
    }
}
