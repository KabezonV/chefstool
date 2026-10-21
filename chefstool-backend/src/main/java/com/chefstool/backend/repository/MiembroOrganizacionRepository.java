package com.chefstool.backend.repository;

import com.chefstool.backend.domain.MiembroOrganizacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MiembroOrganizacionRepository extends JpaRepository<MiembroOrganizacion, UUID> {

    List<MiembroOrganizacion> findByOrganizacionId(UUID organizacionId);
}
