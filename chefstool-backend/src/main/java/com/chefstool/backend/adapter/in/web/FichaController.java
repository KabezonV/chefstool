package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.FichaPdfService;
import com.chefstool.backend.application.FichaService;
import com.chefstool.backend.adapter.in.web.dto.FichaDetalle;
import com.chefstool.backend.adapter.in.web.dto.FichaRequest;
import com.chefstool.backend.adapter.in.web.dto.FichaResumen;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fichas")
public class FichaController {

    private final FichaService fichaService;
    private final FichaPdfService fichaPdfService;

    public FichaController(FichaService fichaService, FichaPdfService fichaPdfService) {
        this.fichaService = fichaService;
        this.fichaPdfService = fichaPdfService;
    }

    /** Escandallo en PDF para imprimir o enviar. */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        FichaDetalle ficha = fichaService.obtener(id);
        byte[] pdf = fichaPdfService.generar(id, null);
        String nombre = "ficha-" + ficha.nombre().replaceAll("[^\\p{L}\\p{N}]+", "-").toLowerCase()
                + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping
    public Pagina<FichaResumen> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String direccion) {
        return fichaService.listar(pagina, tamano, orden, direccion);
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
    @PreAuthorize("@permisos.gestionar()")
    public void eliminar(@PathVariable UUID id) {
        fichaService.eliminar(id);
    }
}
