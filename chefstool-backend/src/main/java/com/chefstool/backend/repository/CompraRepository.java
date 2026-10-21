package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Compra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompraRepository extends JpaRepository<Compra, UUID> {

    List<Compra> findByOrganizacionIdOrderByFechaDesc(UUID organizacionId);
}
