package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.MiembroOrganizacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiembroOrganizacionRepository extends JpaRepository<MiembroOrganizacion, UUID> {

    List<MiembroOrganizacion> findByOrganizacionId(UUID organizacionId);

    Optional<MiembroOrganizacion> findByOrganizacionIdAndUsuarioId(
            UUID organizacionId, UUID usuarioId);

    Optional<MiembroOrganizacion> findFirstByUsuarioIdOrderByCreatedAtAsc(UUID usuarioId);

    long countByOrganizacionIdAndRol(UUID organizacionId, String rol);
}
