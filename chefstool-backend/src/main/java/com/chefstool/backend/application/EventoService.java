package com.chefstool.backend.application;

import com.chefstool.backend.domain.model.Cliente;
import com.chefstool.backend.domain.model.Evento;
import com.chefstool.backend.domain.port.out.ClienteRepository;
import com.chefstool.backend.domain.port.out.EventoRepository;
import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.EventoRequest;
import com.chefstool.backend.adapter.in.web.dto.EventoResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ClienteRepository clienteRepository;
    private final OrganizacionService organizacionService;

    public EventoService(
            EventoRepository eventoRepository,
            ClienteRepository clienteRepository,
            OrganizacionService organizacionService) {
        this.eventoRepository = eventoRepository;
        this.clienteRepository = clienteRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar() {
        return eventoRepository
                .findByOrganizacionIdAndDeletedAtIsNullOrderByFechaDesc(
                        organizacionService.organizacionActual())
                .stream()
                .map(EventoResponse::from)
                .toList();
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Evento evento = new Evento();
        evento.setOrganizacionId(organizacionService.organizacionActual());
        aplicar(evento, request);
        return EventoResponse.from(eventoRepository.save(evento));
    }

    @Transactional
    public EventoResponse actualizar(UUID id, EventoRequest request) {
        Evento evento = buscar(id);
        aplicar(evento, request);
        return EventoResponse.from(eventoRepository.save(evento));
    }

    @Transactional
    public void eliminar(UUID id) {
        buscar(id).setDeletedAt(OffsetDateTime.now());
    }

    private void aplicar(Evento evento, EventoRequest request) {
        evento.setNombre(request.nombre().trim());
        evento.setFecha(request.fecha());
        evento.setComensales(request.comensales());
        evento.setNotas(request.notas());
        evento.setCliente(resolverCliente(request.clienteId()));
    }

    private Cliente resolverCliente(UUID clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteRepository
                .findById(clienteId)
                .filter(cliente -> cliente.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado"));
    }

    private Evento buscar(UUID id) {
        return eventoRepository
                .findById(id)
                .filter(evento -> evento.getDeletedAt() == null)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
    }
}
