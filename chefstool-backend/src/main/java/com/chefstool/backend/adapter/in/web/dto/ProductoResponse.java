package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Producto;
import java.util.UUID;

public record ProductoResponse(UUID id, String nombre, String categoria, String unidadBase) {

    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getCategoria(),
                producto.getUnidadBase());
    }
}
