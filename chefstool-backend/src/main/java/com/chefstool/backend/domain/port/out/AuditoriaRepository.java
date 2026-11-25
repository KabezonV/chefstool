package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Auditoria;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {
    Page<Auditoria> findByOrganizacionIdOrderByCreatedAtDesc(UUID organizacionId, Pageable pageable);
}
