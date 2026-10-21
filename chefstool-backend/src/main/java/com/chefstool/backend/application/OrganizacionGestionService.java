package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.OrganizacionResponse;
import com.chefstool.backend.domain.model.Organizacion;
import com.chefstool.backend.domain.port.out.OrganizacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizacionGestionService {

    private final OrganizacionRepository organizacionRepository;
    private final OrganizacionService organizacionService;

    public OrganizacionGestionService(
            OrganizacionRepository organizacionRepository,
            OrganizacionService organizacionService) {
        this.organizacionRepository = organizacionRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public OrganizacionResponse obtener() {
        return OrganizacionResponse.from(buscar());
    }

    @Transactional
    public OrganizacionResponse renombrar(String nombre) {
        Organizacion organizacion = buscar();
        organizacion.setNombre(nombre.trim());
        return OrganizacionResponse.from(organizacionRepository.save(organizacion));
    }

    private Organizacion buscar() {
        return organizacionRepository
                .findById(organizacionService.organizacionActual())
                .orElseThrow(() -> new RecursoNoEncontradoException("Organizacion no encontrada"));
    }
}
