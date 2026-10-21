package com.chefstool.backend.application;

import com.chefstool.backend.adapter.in.web.RecursoNoEncontradoException;
import com.chefstool.backend.adapter.in.web.dto.MiembroResponse;
import com.chefstool.backend.domain.model.MiembroOrganizacion;
import com.chefstool.backend.domain.port.out.MiembroOrganizacionRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EquipoService {

    private static final Set<String> ROLES = Set.of("propietario", "admin", "miembro");

    private final MiembroOrganizacionRepository miembroRepository;
    private final OrganizacionService organizacionService;

    public EquipoService(
            MiembroOrganizacionRepository miembroRepository,
            OrganizacionService organizacionService) {
        this.miembroRepository = miembroRepository;
        this.organizacionService = organizacionService;
    }

    @Transactional(readOnly = true)
    public List<MiembroResponse> listar() {
        return miembroRepository.findByOrganizacionId(organizacionService.organizacionActual())
                .stream()
                .map(MiembroResponse::from)
                .toList();
    }

    @Transactional
    public MiembroResponse cambiarRol(UUID miembroId, String rol) {
        String nuevoRol = rol == null ? "" : rol.trim().toLowerCase();
        if (!ROLES.contains(nuevoRol)) {
            throw new IllegalArgumentException("Rol no valido: usa propietario, admin o miembro");
        }

        UUID organizacionId = organizacionService.organizacionActual();
        MiembroOrganizacion miembro =
                miembroRepository
                        .findById(miembroId)
                        .filter(m -> m.getOrganizacionId().equals(organizacionId))
                        .orElseThrow(() -> new RecursoNoEncontradoException("Miembro no encontrado"));

        boolean eraPropietario = "propietario".equals(miembro.getRol());
        if (eraPropietario
                && !"propietario".equals(nuevoRol)
                && miembroRepository.countByOrganizacionIdAndRol(organizacionId, "propietario") <= 1) {
            throw new IllegalArgumentException(
                    "La organizacion necesita al menos un propietario");
        }

        miembro.setRol(nuevoRol);
        return MiembroResponse.from(miembroRepository.save(miembro));
    }
}
