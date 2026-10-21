package com.chefstool.backend.repository;

import com.chefstool.backend.domain.AlertaPrecio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaPrecioRepository extends JpaRepository<AlertaPrecio, UUID> {

    List<AlertaPrecio> findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(UUID organizacionId);
}
