package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.AlertaPrecio;
import com.chefstool.backend.domain.port.out.AlertaPrecioRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.AlertaPrecioResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlertaPrecioService {

    private final AlertaPrecioRepository alertaRepository;
    private final OrganizacionService organizacionService;

    public AlertaPrecioService(
            AlertaPrecioRepository alertaRepository, OrganizacionService organizacionService) {
        this.alertaRepository = alertaRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<AlertaPrecioResponse> pendientes() {
        return alertaRepository
                .findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(
                        organizacionService.organizacionActual())
                .stream()
                .map(AlertaPrecioResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlertaPrecioResponse> todas() {
        return alertaRepository
                .todas(organizacionService.organizacionActual())
                .stream()
                .map(AlertaPrecioResponse::from)
                .toList();
    }

    @Transactional
    public void reconocer(UUID id) {
        AlertaPrecio alerta =
                alertaRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new RecursoNoEncontradoException("Alerta no encontrada"));
        alerta.setReconocida(true);
        alertaRepository.save(alerta);
    }
}
