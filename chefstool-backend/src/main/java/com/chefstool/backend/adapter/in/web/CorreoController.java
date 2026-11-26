package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.CorreoService;
import com.chefstool.backend.application.EstadoCorreo;
import com.chefstool.backend.application.ResultadoEnvio;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Estado del correo saliente y envio de una prueba, sin exponer credenciales. */
@RestController
@RequestMapping("/api/correo")
public class CorreoController {

    private final CorreoService correoService;
    private final UsuarioRepository usuarioRepository;

    public CorreoController(CorreoService correoService, UsuarioRepository usuarioRepository) {
        this.correoService = correoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/estado")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public EstadoCorreo estado() {
        return correoService.estado();
    }

    /** Manda un correo de prueba a la cuenta con la que se ha entrado. */
    @PostMapping("/prueba")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public ResultadoEnvio prueba(@AuthenticationPrincipal Jwt jwt) {
        String destinatario = usuarioRepository
                .findById(UUID.fromString(jwt.getSubject()))
                .map(usuario -> usuario.getEmail())
                .orElse(null);
        return correoService.probar(destinatario);
    }
}
