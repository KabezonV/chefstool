package com.chefstool.backend.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Resuelve la organizacion activa a partir del token JWT. Si no hay autenticacion
 * (por ejemplo en tests con la seguridad desactivada) usa la organizacion demo.
 */
@Service
public class OrganizacionService {

    private static final UUID ORGANIZACION_POR_DEFECTO =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    public UUID organizacionActual() {
        return jwt()
                .map(jwt -> jwt.getClaimAsString("organizacionId"))
                .filter(valor -> valor != null && !valor.isBlank())
                .map(UUID::fromString)
                .orElse(ORGANIZACION_POR_DEFECTO);
    }

    public Optional<UUID> usuarioActual() {
        return jwt().map(jwt -> UUID.fromString(jwt.getSubject()));
    }

    private Optional<Jwt> jwt() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}
