package com.chefstool.backend.adapter.in.web.dto;

import com.chefstool.backend.domain.model.Evento;
import java.time.LocalDate;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        LocalDate fecha,
        Integer comensales,
        UUID clienteId,
        String clienteNombre,
        String notas) {

    public static EventoResponse from(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getNombre(),
                evento.getFecha(),
                evento.getComensales(),
                evento.getCliente() != null ? evento.getCliente().getId() : null,
                evento.getCliente() != null ? evento.getCliente().getNombre() : null,
                evento.getNotas());
    }
}
