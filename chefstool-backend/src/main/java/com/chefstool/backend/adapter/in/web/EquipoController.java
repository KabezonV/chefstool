package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.adapter.in.web.dto.CambioRolRequest;
import com.chefstool.backend.adapter.in.web.dto.InvitacionRequest;
import com.chefstool.backend.adapter.in.web.dto.InvitacionResponse;
import com.chefstool.backend.adapter.in.web.dto.MiembroResponse;
import com.chefstool.backend.application.EquipoService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    @PostMapping("/invitar")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public InvitacionResponse invitar(@Valid @RequestBody InvitacionRequest peticion) {
        return equipoService.invitar(peticion);
    }

    @GetMapping("/invitaciones")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public List<InvitacionResponse> invitaciones() {
        return equipoService.invitacionesPendientes();
    }

    @DeleteMapping("/invitaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public void cancelarInvitacion(@PathVariable UUID id) {
        equipoService.cancelarInvitacion(id);
    }

    @PutMapping("/{id}/rol")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public MiembroResponse cambiarRol(
            @PathVariable UUID id, @Valid @RequestBody CambioRolRequest peticion) {
        return equipoService.cambiarRol(id, peticion.rol());
    }
}
