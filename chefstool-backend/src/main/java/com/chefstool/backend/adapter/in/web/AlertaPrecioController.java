package com.chefstool.backend.adapter.in.web;

import com.chefstool.backend.application.AlertaCorreoProgramada;
import com.chefstool.backend.application.AlertaPrecioService;
import com.chefstool.backend.adapter.in.web.dto.AlertaPrecioResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/alertas")
public class AlertaPrecioController {

    private final AlertaPrecioService alertaService;
    private final AlertaCorreoProgramada correoProgramado;

    public AlertaPrecioController(
            AlertaPrecioService alertaService,
            AlertaCorreoProgramada correoProgramado) {
        this.alertaService = alertaService;
        this.correoProgramado = correoProgramado;
    }

    @GetMapping
    public List<AlertaPrecioResponse> listar(
            @RequestParam(defaultValue = "false") boolean todas) {
        return todas ? alertaService.todas() : alertaService.pendientes();
    }

    @PostMapping("/correo/enviar")
    @PreAuthorize("hasAnyRole('propietario','admin')")
    public int enviarCorreo() {
        return correoProgramado.enviarResumenSemanal();
    }

    @PostMapping("/{id}/reconocer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reconocer(@PathVariable UUID id) {
        alertaService.reconocer(id);
    }
}
