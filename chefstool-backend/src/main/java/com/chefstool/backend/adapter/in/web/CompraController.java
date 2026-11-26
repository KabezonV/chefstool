package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.CompraPaqueteService;
import com.chefstool.backend.application.CompraPdfService;
import com.chefstool.backend.application.CompraService;
import com.chefstool.backend.adapter.in.web.dto.CompraDetalle;
import com.chefstool.backend.adapter.in.web.dto.CompraResumen;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final CompraPdfService compraPdfService;
    private final CompraPaqueteService compraPaqueteService;

    public CompraController(
            CompraService compraService,
            CompraPdfService compraPdfService,
            CompraPaqueteService compraPaqueteService) {
        this.compraService = compraService;
        this.compraPdfService = compraPdfService;
        this.compraPaqueteService = compraPaqueteService;
    }

    @GetMapping
    public Pagina<CompraResumen> listar(
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate hasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String direccion) {
        return compraService.listar(
                proveedorId, desde, hasta, pagina, tamano, orden, direccion);
    }

    @GetMapping("/{id}")
    public CompraDetalle obtener(@PathVariable UUID id) {
        return compraService.obtener(id);
    }

    /** Resumen imprimible de la compra (proveedor, lineas y totales). */
    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        CompraDetalle compra = compraService.obtener(id);
        byte[] pdf = compraPdfService.generar(id);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombrePdf(compra), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    /** Albaranes originales de las compras del periodo, opcionalmente de un proveedor. */
    @GetMapping("/zip")
    public ResponseEntity<byte[]> zip(
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate hasta) {
        byte[] zip = compraPaqueteService.descargarZip(proveedorId, desde, hasta);
        String nombre = "compras-%s_%s.zip".formatted(desde, hasta);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(nombre, StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    private String nombrePdf(CompraDetalle compra) {
        String referencia = compra.numeroDocumento() == null || compra.numeroDocumento().isBlank()
                ? compra.fecha() == null ? "compra" : compra.fecha().toString()
                : compra.numeroDocumento();
        String limpio = referencia.replaceAll("[^\\p{L}\\p{N}]+", "-");
        return "compra-" + limpio + ".pdf";
    }
}
