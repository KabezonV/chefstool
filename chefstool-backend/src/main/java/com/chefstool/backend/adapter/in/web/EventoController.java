package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.EventoService;
import com.chefstool.backend.adapter.in.web.dto.EventoRequest;
import com.chefstool.backend.adapter.in.web.dto.EventoResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import com.chefstool.backend.application.dto.Pagina;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public Pagina<EventoResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String direccion) {
        return eventoService.listar(pagina, tamano, q, orden, direccion);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventoResponse crear(@Valid @RequestBody EventoRequest request) {
        return eventoService.crear(request);
    }

    @PutMapping("/{id}")
    public EventoResponse actualizar(
            @PathVariable UUID id, @Valid @RequestBody EventoRequest request) {
        return eventoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        eventoService.eliminar(id);
    }
}
