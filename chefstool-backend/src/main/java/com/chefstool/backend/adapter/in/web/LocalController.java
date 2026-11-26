package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.LocalRequest;
import com.chefstool.backend.adapter.in.web.dto.LocalResponse;
import com.chefstool.backend.adapter.in.web.dto.SesionResponse;
import com.chefstool.backend.application.AuthService;
import com.chefstool.backend.application.LocalService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/locales")
public class LocalController {
    private final LocalService localService;
    private final AuthService authService;

    public LocalController(LocalService localService, AuthService authService) {
        this.localService = localService;
        this.authService = authService;
    }

    @GetMapping
    public List<LocalResponse> listar() {
        return localService.listar();
    }

    @PostMapping("/{id}/seleccionar")
    public SesionResponse seleccionar(
            @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return authService.cambiarLocal(UUID.fromString(jwt.getSubject()), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public LocalResponse crear(@Valid @RequestBody LocalRequest request) {
        return localService.crear(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public void eliminar(@PathVariable UUID id) {
        localService.eliminar(id);
    }
}
