package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.CompraService;
import com.chefstool.backend.adapter.in.web.dto.CompraDetalle;
import com.chefstool.backend.adapter.in.web.dto.CompraResumen;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import com.chefstool.backend.application.dto.Pagina;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compras")
public class CompraController {

    private final CompraService compraService;

    public CompraController(CompraService compraService) {
        this.compraService = compraService;
    }

    @GetMapping
    public Pagina<CompraResumen> listar(
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return compraService.listar(proveedorId, desde, hasta, pagina, tamano);
    }

    @GetMapping("/{id}")
    public CompraDetalle obtener(@PathVariable UUID id) {
        return compraService.obtener(id);
    }
}
