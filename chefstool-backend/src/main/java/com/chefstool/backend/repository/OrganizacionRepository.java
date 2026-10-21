package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Organizacion;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizacionRepository extends JpaRepository<Organizacion, UUID> {}
