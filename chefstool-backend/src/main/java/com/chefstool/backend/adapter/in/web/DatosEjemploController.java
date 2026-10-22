package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.DatosEjemploService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
public class DatosEjemploController {

    private final DatosEjemploService datosEjemploService;

    public DatosEjemploController(DatosEjemploService datosEjemploService) {
        this.datosEjemploService = datosEjemploService;
    }

    @PostMapping("/datos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public DatosEjemploService.Resumen cargar() {
        return datosEjemploService.cargar();
    }
}
