package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.DocumentoCompra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoCompraRepository extends JpaRepository<DocumentoCompra, UUID> {

    List<DocumentoCompra> findByOrganizacionIdOrderByCreatedAtDesc(UUID organizacionId);

    List<DocumentoCompra> findByOrganizacionIdAndEstadoOrderByCreatedAtDesc(
            UUID organizacionId, String estado);

    long countByOrganizacionIdAndEstado(UUID organizacionId, String estado);

    org.springframework.data.domain.Page<DocumentoCompra> findByOrganizacionId(
            UUID organizacionId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<DocumentoCompra> findByOrganizacionIdAndLocalId(
            UUID organizacionId, UUID localId, org.springframework.data.domain.Pageable pageable);

    /** Documentos con archivo de un periodo, para descargarlos juntos en un ZIP. */
    List<DocumentoCompra> findByOrganizacionIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID organizacionId, java.time.OffsetDateTime desde, java.time.OffsetDateTime hasta);

    List<DocumentoCompra> findByOrganizacionIdAndLocalIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            UUID organizacionId,
            UUID localId,
            java.time.OffsetDateTime desde,
            java.time.OffsetDateTime hasta);
}
