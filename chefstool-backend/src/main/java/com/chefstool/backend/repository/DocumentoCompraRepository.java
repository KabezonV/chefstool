package com.chefstool.backend.repository;

import com.chefstool.backend.domain.DocumentoCompra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoCompraRepository extends JpaRepository<DocumentoCompra, UUID> {

    List<DocumentoCompra> findByOrganizacionIdOrderByCreatedAtDesc(UUID organizacionId);

    List<DocumentoCompra> findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(
            UUID organizacionId, String estado);

    long countByOrganizacionIdAndEstado(UUID organizacionId, String estado);
}
