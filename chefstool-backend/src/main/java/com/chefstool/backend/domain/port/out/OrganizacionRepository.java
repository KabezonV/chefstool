package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Organizacion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacionRepository extends JpaRepository<Organizacion, UUID> {}
