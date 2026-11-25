package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.LocalRequest;
import com.chefstool.backend.adapter.in.web.dto.LocalResponse;
import com.chefstool.backend.domain.model.Local;
import com.chefstool.backend.domain.port.out.LocalRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LocalService {
    private final LocalRepository localRepository;
    private final OrganizacionService organizacionService;

    public LocalService(LocalRepository localRepository, OrganizacionService organizacionService) {
        this.localRepository = localRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<LocalResponse> listar() {
        return localRepository
                .findByOrganizacionIdAndDeletedAtIsNullAndActivoTrueOrderByNombreAsc(
                        organizacionService.organizacionActual())
                .stream()
                .map(LocalResponse::from)
                .toList();
    }

    @Transactional
    public LocalResponse crear(LocalRequest request) {
        UUID organizacionId = organizacionService.organizacionActual();
        String nombre = request.nombre().trim();
        if (localRepository.existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
                organizacionId, nombre)) {
            throw new IllegalArgumentException("Ya existe un local llamado " + nombre);
        }
        Local local = new Local();
        local.setOrganizacionId(organizacionId);
        local.setNombre(nombre);
        return LocalResponse.from(localRepository.save(local));
    }

    @Transactional
    public void eliminar(UUID id) {
        Local local = buscar(id);
        local.setActivo(false);
        local.setDeletedAt(OffsetDateTime.now());
        localRepository.save(local);
    }

    @Transactional(readOnly = true)
    public Local buscar(UUID id) {
        return localRepository
                .findByIdAndOrganizacionIdAndDeletedAtIsNull(
                        id, organizacionService.organizacionActual())
                .orElseThrow(() -> new RecursoNoEncontradoException("Local no encontrado"));
    }

    @Transactional(readOnly = true)
    public UUID principalId() {
        UUID organizacionId = organizacionService.organizacionActual();
        return localRepository
                .findByOrganizacionIdAndDeletedAtIsNullAndActivoTrueOrderByNombreAsc(organizacionId)
                .stream()
                .findFirst()
                .map(Local::getId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La organización no tiene locales"));
    }
}
