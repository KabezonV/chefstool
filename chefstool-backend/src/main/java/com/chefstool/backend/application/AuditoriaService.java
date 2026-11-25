package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.AuditoriaResponse;
import com.chefstool.backend.application.dto.Pagina;
import com.chefstool.backend.domain.model.Auditoria;
import com.chefstool.backend.domain.port.out.AuditoriaRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final OrganizacionService organizacionService;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            OrganizacionService organizacionService) {
        this.auditoriaRepository = auditoriaRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional
    public void registrar(String accion, String entidad, UUID entidadId, String detalle) {
        Auditoria evento = new Auditoria();
        evento.setOrganizacionId(organizacionService.organizacionActual());
        evento.setUsuarioId(organizacionService.usuarioActual().orElse(null));
        evento.setAccion(accion);
        evento.setEntidad(entidad);
        evento.setEntidadId(entidadId);
        evento.setDetalle(detalle);
        auditoriaRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public Pagina<AuditoriaResponse> listar(int pagina, int tamano) {
        Pageable pageable = Pagina.peticion(
                pagina, tamano, Sort.by("createdAt").descending());
        return Pagina.desde(
                        auditoriaRepository.findByOrganizacionIdOrderByCreatedAtDesc(
                                organizacionService.organizacionActual(), pageable))
                .map(AuditoriaResponse::from);
    }
}
