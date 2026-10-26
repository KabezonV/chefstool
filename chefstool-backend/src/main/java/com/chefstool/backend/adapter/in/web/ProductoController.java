package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.ImportacionProductosService;
import com.chefstool.backend.application.ProductoService;
import com.chefstool.backend.adapter.in.web.dto.HistoricoPrecios;
import com.chefstool.backend.adapter.in.web.dto.ProductoRequest;
import com.chefstool.backend.adapter.in.web.dto.ProductoResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final ImportacionProductosService importacionService;

    public ProductoController(
            ProductoService productoService,
            ImportacionProductosService importacionService) {
        this.productoService = productoService;
        this.importacionService = importacionService;
    }

    /** Alta masiva desde el CSV que exporta Excel. */
    @PostMapping(value = "/importar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacionProductosService.Resumen importar(
            @RequestPart("archivo") MultipartFile archivo) {
        return importacionService.importar(archivo);
    }

    @GetMapping
    public Pagina<ProductoResponse> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String direccion) {
        return productoService.listar(pagina, tamano, q, orden, direccion);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoResponse crear(@Valid @RequestBody ProductoRequest request) {
        return productoService.crear(request);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(
            @PathVariable UUID id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }

    @GetMapping("/{id}/precios")
    public HistoricoPrecios precios(@PathVariable UUID id) {
        return productoService.historico(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permisos.gestionar()")
    public void eliminar(@PathVariable UUID id) {
        productoService.eliminar(id);
    }
}
