package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.ConflictoException;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.RechazoSolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraRequest;
import com.chefstool.backend.adapter.in.web.dto.SolicitudCompraResponse;
import com.chefstool.backend.domain.model.EstadoSolicitudCompra;
import com.chefstool.backend.domain.model.SolicitudCompra;
import com.chefstool.backend.domain.model.Usuario;
import com.chefstool.backend.domain.port.out.SolicitudCompraRepository;
import com.chefstool.backend.domain.port.out.UsuarioRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SolicitudCompraService {

    private final SolicitudCompraRepository solicitudRepository;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacionService organizacionService;
    private final AuditoriaService auditoriaService;

    public SolicitudCompraService(
            SolicitudCompraRepository solicitudRepository,
            UsuarioRepository usuarioRepository,
            OrganizacionService organizacionService,
            AuditoriaService auditoriaService) {
        this.solicitudRepository = solicitudRepository;
        this.usuarioRepository = usuarioRepository;
        this.organizacionService = organizacionService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<SolicitudCompraResponse> listar() {
        UUID organizacionId = organizacionService.organizacionActual();
        UUID localId = organizacionService.localActual();
        if (localId == null) {
            return List.of();
        }
        return respuestas(
                solicitudRepository.findByOrganizacionIdAndLocalIdOrderByCreatedAtDesc(
                        organizacionId, localId));
    }

    @Transactional
    public SolicitudCompraResponse crear(SolicitudCompraRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        UUID localId = organizacionService.localActual();
        if (localId == null) {
            throw new RecursoNoEncontradoException("No hay un local activo para esta organización");
        }

        SolicitudCompra solicitud = new SolicitudCompra();
        solicitud.setOrganizacionId(organizacionId);
        solicitud.setLocalId(localId);
        solicitud.setSolicitanteId(organizacionService.usuarioActual().orElse(null));
        solicitud.setConcepto(request.concepto().trim());
        solicitud.setDescripcion(normalizar(request.descripcion()));
        solicitud.setProveedor(normalizar(request.proveedor()));
        solicitud.setImporte(request.importe());
        SolicitudCompra guardada = solicitudRepository.save(solicitud);

        auditoriaService.registrar(
                "SOLICITAR",
                "SOLICITUD_COMPRA",
                guardada.getId(),
                "%s — %s EUR".formatted(guardada.getConcepto(), guardada.getImporte()));
        return respuesta(guardada);
    }

    @Transactional
    public SolicitudCompraResponse aprobar(UUID id) {
        SolicitudCompra solicitud = buscar(id);
        validarPendiente(solicitud);
        UUID actorId = organizacionService.usuarioActual().orElse(null);
        validarNoEsSolicitante(solicitud, actorId);

        solicitud.setEstado(EstadoSolicitudCompra.APROBADA);
        solicitud.setRevisadaPor(actorId);
        solicitud.setRevisadaAt(OffsetDateTime.now());
        SolicitudCompra guardada = solicitudRepository.save(solicitud);
        auditoriaService.registrar(
                "APROBAR", "SOLICITUD_COMPRA", guardada.getId(), guardada.getConcepto());
        return respuesta(guardada);
    }

    @Transactional
    public SolicitudCompraResponse rechazar(UUID id, RechazoSolicitudCompraRequest request) {
        SolicitudCompra solicitud = buscar(id);
        validarPendiente(solicitud);
        UUID actorId = organizacionService.usuarioActual().orElse(null);
        validarNoEsSolicitante(solicitud, actorId);

        String motivo = request.motivo().trim();
        if (motivo.isEmpty()) {
            throw new IllegalArgumentException("Indica el motivo del rechazo");
        }
        solicitud.setEstado(EstadoSolicitudCompra.RECHAZADA);
        solicitud.setRevisadaPor(actorId);
        solicitud.setRevisadaAt(OffsetDateTime.now());
        solicitud.setMotivoRechazo(motivo);
        SolicitudCompra guardada = solicitudRepository.save(solicitud);
        auditoriaService.registrar(
                "RECHAZAR",
                "SOLICITUD_COMPRA",
                guardada.getId(),
                "%s — %s".formatted(guardada.getConcepto(), motivo));
        return respuesta(guardada);
    }

    private SolicitudCompra buscar(UUID id) {
        UUID localId = organizacionService.localActual();
        if (localId == null) {
            throw new RecursoNoEncontradoException("Solicitud de compra no encontrada");
        }
        return solicitudRepository
                .buscarParaResolver(
                        id, organizacionService.organizacionActual(), localId)
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Solicitud de compra no encontrada"));
    }

    private void validarPendiente(SolicitudCompra solicitud) {
        if (solicitud.getEstado() != EstadoSolicitudCompra.PENDIENTE) {
            throw new ConflictoException("La solicitud ya ha sido resuelta");
        }
    }

    private void validarNoEsSolicitante(SolicitudCompra solicitud, UUID actorId) {
        if (actorId != null && actorId.equals(solicitud.getSolicitanteId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "No puedes resolver tu propia solicitud de compra");
        }
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private SolicitudCompraResponse respuesta(SolicitudCompra solicitud) {
        return respuestas(List.of(solicitud)).getFirst();
    }

    private List<SolicitudCompraResponse> respuestas(List<SolicitudCompra> solicitudes) {
        List<UUID> ids =
                solicitudes.stream()
                        .flatMap(
                                solicitud ->
                                        java.util.stream.Stream.of(
                                                solicitud.getSolicitanteId(),
                                                solicitud.getRevisadaPor()))
                        .filter(id -> id != null)
                        .distinct()
                        .toList();
        Map<UUID, String> nombres =
                usuarioRepository.findAllById(ids).stream()
                        .collect(
                                Collectors.toMap(
                                        Usuario::getId,
                                        this::nombreUsuario,
                                        (primero, ignorado) -> primero));

        return solicitudes.stream()
                .map(
                        solicitud ->
                                SolicitudCompraResponse.from(
                                        solicitud,
                                        nombres.get(solicitud.getSolicitanteId()),
                                        nombres.get(solicitud.getRevisadaPor())))
                .toList();
    }

    private String nombreUsuario(Usuario usuario) {
        return usuario.getNombre() == null || usuario.getNombre().isBlank()
                ? usuario.getEmail()
                : usuario.getNombre();
    }
}
