package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Cliente;
import java.util.UUID;

public record ClienteResponse(UUID id, String nombre, String email, String telefono, String notas) {

    public static ClienteResponse from(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getEmail(),
                cliente.getTelefono(),
                cliente.getNotas());
    }
}
