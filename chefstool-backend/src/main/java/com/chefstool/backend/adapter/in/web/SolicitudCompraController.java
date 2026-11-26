package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.RechazoSolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraResponse;
import com.chefstool.backend.application.SolicitudCompraService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solicitudes-compra")
public class SolicitudCompraController {

    private final SolicitudCompraService solicitudCompraService;

    public SolicitudCompraController(SolicitudCompraService solicitudCompraService) {
        this.solicitudCompraService = solicitudCompraService;
    }

    @GetMapping
    public List<SolicitudCompraResponse> listar() {
        return solicitudCompraService.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudCompraResponse crear(@Valid @RequestBody SolicitudCompraRequest request) {
        return solicitudCompraService.crear(request);
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public SolicitudCompraResponse aprobar(@PathVariable UUID id) {
        return solicitudCompraService.aprobar(id);
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public SolicitudCompraResponse rechazar(
            @PathVariable UUID id, @Valid @RequestBody RechazoSolicitudCompraRequest request) {
        return solicitudCompraService.rechazar(id, request);
    }
}
