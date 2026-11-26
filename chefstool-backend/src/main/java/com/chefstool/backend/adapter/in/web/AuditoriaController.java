package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.AuditoriaResponse;
import com.chefstool.backend.application.AuditoriaService;
import com.chefstool.backend.application.dto.Pagina;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {
    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public Pagina<AuditoriaResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return auditoriaService.listar(pagina, tamano);
    }
}
