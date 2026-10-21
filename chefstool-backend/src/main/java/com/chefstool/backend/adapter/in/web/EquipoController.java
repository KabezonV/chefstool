package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.CambioRolRequest;
import com.chefstool.backend.adapter.in.web.dto.MiembroResponse;
import com.chefstool.backend.application.EquipoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public List<MiembroResponse> listar() {
        return equipoService.listar();
    }

    @PutMapping("/{id}/rol")
    public MiembroResponse cambiarRol(
            @PathVariable UUID id, @Valid @RequestBody CambioRolRequest peticion) {
        return equipoService.cambiarRol(id, peticion.rol());
    }
}
