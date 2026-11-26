package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.SolicitudCompra;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudCompraRepository extends JpaRepository<SolicitudCompra, UUID> {

    List<SolicitudCompra> findByOrganizacionIdAndLocalIdOrderByCreatedAtDesc(
            UUID organizacionId, UUID localId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select s from SolicitudCompra s
            where s.id = :id
              and s.organizacionId = :organizacionId
              and s.localId = :localId
            """)
    Optional<SolicitudCompra> buscarParaResolver(
            @Param("id") UUID id,
            @Param("organizacionId") UUID organizacionId,
            @Param("localId") UUID localId);
}
