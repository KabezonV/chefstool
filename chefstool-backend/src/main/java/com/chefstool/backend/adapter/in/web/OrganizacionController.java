package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.OrganizacionRequest;
import com.chefstool.backend.adapter.in.web.dto.OrganizacionResponse;
import com.chefstool.backend.application.OrganizacionGestionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizacion")
public class OrganizacionController {

    private final OrganizacionGestionService organizacionService;

    public OrganizacionController(OrganizacionGestionService organizacionService) {
        this.organizacionService = organizacionService;
    }

    @GetMapping
    public OrganizacionResponse obtener() {
        return organizacionService.obtener();
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public OrganizacionResponse renombrar(@Valid @RequestBody OrganizacionRequest peticion) {
        return organizacionService.renombrar(peticion.nombre());
    }
}
