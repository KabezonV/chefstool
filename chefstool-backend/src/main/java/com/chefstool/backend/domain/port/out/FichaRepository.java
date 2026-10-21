package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Ficha;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaRepository extends JpaRepository<Ficha, UUID> {

    List<Ficha> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);
}
