package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.LoginRequest;
import com.chefstool.backend.adapter.in.web.dto.RegistroRequest;
import com.chefstool.backend.adapter.in.web.dto.SesionResponse;
import com.chefstool.backend.application.AuthService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public SesionResponse registrar(@Valid @RequestBody RegistroRequest peticion) {
        return authService.registrar(peticion);
    }

    @PostMapping("/login")
    public SesionResponse login(@Valid @RequestBody LoginRequest peticion) {
        return authService.login(peticion);
    }

    @GetMapping("/yo")
    public SesionResponse yo(@AuthenticationPrincipal Jwt jwt) {
        return authService.sesionActual(UUID.fromString(jwt.getSubject()));
    }
}
