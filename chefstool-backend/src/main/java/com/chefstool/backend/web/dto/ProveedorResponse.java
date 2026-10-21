package com.chefstool.backend.web.dto;

import com.chefstool.backend.domain.Proveedor;
import java.util.UUID;

public record ProveedorResponse(
        UUID id, String nombre, String cif, String email, String telefono, String notas) {

    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getNombre(),
                proveedor.getCif(),
                proveedor.getEmail(),
                proveedor.getTelefono(),
                proveedor.getNotas());
    }
}
