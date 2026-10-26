package com.chefstool.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Reglas de permisos reutilizables en {@code @PreAuthorize}.
 *
 * <p>Se resuelven aqui y no con SpEL suelto para poder tener en cuenta si la seguridad
 * esta desactivada (perfil de test): en ese caso todo vale y las pruebas de los servicios
 * no necesitan montar un usuario.
 */
@Component("permisos")
public class Permisos {

    private final boolean seguridadHabilitada;

    public Permisos(
            @Value("${chefstool.seguridad.habilitada:true}") boolean seguridadHabilitada) {
        this.seguridadHabilitada = seguridadHabilitada;
    }

    /** Propietario o administrador: gestionar equipo, configuracion y catalogo. */
    public boolean gestionar() {
        if (!seguridadHabilitada) {
            return true;
        }
        return tieneRol("propietario") || tieneRol("admin");
    }

    private boolean tieneRol(String rol) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null || !autenticacion.isAuthenticated()) {
            return false;
        }
        String buscado = "ROLE_" + rol;
        for (GrantedAuthority autoridad : autenticacion.getAuthorities()) {
            if (buscado.equalsIgnoreCase(autoridad.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
