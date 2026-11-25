package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.AlertaPrecio;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.port.out.AlertaPrecioRepository;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertaCorreoProgramada {
    private static final Logger log = LoggerFactory.getLogger(AlertaCorreoProgramada.class);

    private final AlertaPrecioRepository alertaRepository;
    private final MiembroOrganizacionRepository miembroRepository;
    private final CorreoService correoService;

    public AlertaCorreoProgramada(
            AlertaPrecioRepository alertaRepository,
            MiembroOrganizacionRepository miembroRepository,
            CorreoService correoService) {
        this.alertaRepository = alertaRepository;
        this.miembroRepository = miembroRepository;
        this.correoService = correoService;
    }

    @Scheduled(cron = "${chefstool.alertas.correo.cron:0 0 8 * * MON}")
    public void semanal() {
        enviarResumenSemanal();
    }

    public int enviarResumenSemanal() {
        if (!correoService.disponible()) {
            log.debug("El correo SMTP no está configurado; no se envían alertas");
            return 0;
        }

        List<AlertaPrecio> pendientes = alertaRepository.findAll().stream()
                .filter(alerta -> !alerta.isReconocida())
                .toList();
        if (pendientes.isEmpty()) {
            return 0;
        }

        int enviados = 0;
        for (UUID organizacionId : pendientes.stream()
                .map(AlertaPrecio::getOrganizacionId)
                .collect(Collectors.toSet())) {
            List<AlertaPrecio> deOrganizacion = pendientes.stream()
                    .filter(alerta -> alerta.getOrganizacionId().equals(organizacionId))
                    .toList();
            String texto = deOrganizacion.stream()
                    .map(AlertaPrecio::getMensaje)
                    .distinct()
                    .collect(Collectors.joining("\n- ", "- ", ""));

            for (MiembroOrganizacion miembro : miembroRepository.findByOrganizacionId(organizacionId)) {
                if ("miembro".equals(miembro.getRol())) {
                    continue;
                }
                if (correoService.enviar(
                        miembro.getEmail(),
                        "Resumen de alertas de precios - Chefstool",
                        "Hay " + deOrganizacion.size() + " alertas pendientes:\n" + texto)) {
                    enviados++;
                }
            }
        }
        return enviados;
    }
}
