package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.FichaService;
import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import com.chefstool.backend.adapter.in.web.dto.FichaRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaResumen;
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
@RequestMapping("/api/fichas")
public class FichaController {

    private final FichaService fichaService;

    public FichaController(FichaService fichaService) {
        this.fichaService = fichaService;
    }

    @GetMapping
    public Pagina<FichaResumen> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return fichaService.listar(pagina, tamano);
    }

    @GetMapping("/{id}")
    public FichaDetalle obtener(@PathVariable UUID id) {
        return fichaService.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FichaDetalle crear(@Valid @RequestBody FichaRequest request) {
        return fichaService.crear(request);
    }

    @PutMapping("/{id}")
    public FichaDetalle actualizar(
            @PathVariable UUID id, @Valid @RequestBody FichaRequest request) {
        return fichaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable UUID id) {
        fichaService.eliminar(id);
    }
}
