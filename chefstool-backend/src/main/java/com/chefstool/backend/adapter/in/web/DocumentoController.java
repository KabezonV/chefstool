package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.DocumentoCompraService;
import com.chefstool.backend.adapter.in.web.dto.ArchivoDescargable;
import com.chefstool.backend.adapter.in.web.dto.CompraResponse;
import com.chefstool.backend.adapter.in.web.dto.DocumentoDetalle;
import com.chefstool.backend.adapter.in.web.dto.DocumentoResumen;
import com.chefstool.backend.domain.model.ExtraccionDocumento;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoCompraService documentoService;

    public DocumentoController(DocumentoCompraService documentoService) {
        this.documentoService = documentoService;
    }

    @GetMapping
    public Pagina<DocumentoResumen> listar(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        return documentoService.listar(pagina, tamano);
    }

    @GetMapping("/{id}")
    public DocumentoDetalle obtener(@PathVariable UUID id) {
        return documentoService.obtener(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentoDetalle subir(
            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
            @RequestParam(value = "origen", defaultValue = "subida") String origen) {
        return documentoService.subir(archivo, origen);
    }

    @PutMapping("/{id}")
    public DocumentoDetalle guardarBorrador(
            @PathVariable UUID id, @RequestBody ExtraccionDocumento extraccion) {
        return documentoService.guardarBorrador(id, extraccion);
    }

    @PostMapping("/{id}/confirmar")
    public CompraResponse confirmar(
            @PathVariable UUID id, @RequestBody ExtraccionDocumento extraccion) {
        return documentoService.confirmar(id, extraccion);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void descartar(@PathVariable UUID id) {
        documentoService.descartar(id);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<org.springframework.core.io.Resource> archivo(@PathVariable UUID id) {
        ArchivoDescargable archivo = documentoService.archivo(id);
        return ResponseEntity.ok()
                .contentType(
                        archivo.tipoMime() == null
                                ? MediaType.APPLICATION_OCTET_STREAM
                                : MediaType.parseMediaType(archivo.tipoMime()))
                .header(
                        "Content-Disposition",
                        "inline; filename=\"%s\"".formatted(archivo.nombre()))
                .body(archivo.recurso());
    }
}
