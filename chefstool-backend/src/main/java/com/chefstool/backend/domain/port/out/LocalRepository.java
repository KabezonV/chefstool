package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Local;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalRepository extends JpaRepository<Local, UUID> {
    List<Local> findByOrganizacionIdAndDeletedAtIsNullAndActivoTrueOrderByNombreAsc(UUID organizacionId);
    Optional<Local> findByIdAndOrganizacionIdAndDeletedAtIsNull(UUID id, UUID organizacionId);
    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(UUID organizacionId, String nombre);
}
