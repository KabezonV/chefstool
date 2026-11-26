package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.dto.AuditoriaResponse;
import com.chefstool.backend.application.dto.Pagina;
import com.chefstool.backend.domain.model.Auditoria;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.AuditoriaRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionService organizacionService;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository,
            OrganizacionService organizacionService) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
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
        var eventos = auditoriaRepository.findByOrganizacionIdOrderByCreatedAtDesc(
                organizacionService.organizacionActual(), pageable);

        // Un UUID no le dice nada a nadie: se resuelven los nombres de los implicados
        Set<UUID> usuarios = eventos.getContent().stream()
                .map(Auditoria::getUsuarioId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<UUID, String> nombres = usuarios.isEmpty()
                ? Map.of()
                : usuarioRepository.findAllById(usuarios).stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Usuario::getId, Usuario::getNombre));

        return Pagina.desde(eventos)
                .map(evento -> AuditoriaResponse.from(evento, nombres.get(evento.getUsuarioId())));
    }
}
