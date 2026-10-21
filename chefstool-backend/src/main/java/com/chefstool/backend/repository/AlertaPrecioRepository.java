package com.chefstool.backend.repository;

import com.chefstool.backend.domain.AlertaPrecio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertaPrecioRepository extends JpaRepository<AlertaPrecio, UUID> {

    List<AlertaPrecio> findByOrganizacionIdAndReconocidaFalseOrderByCreatedAtDesc(UUID organizacionId);

    @Query(
            """
            select a from AlertaPrecio a
            left join fetch a.producto
            left join fetch a.proveedor
            where a.organizacionId = :organizacionId
            order by a.createdAt desc
            """)
    List<AlertaPrecio> todas(@Param("organizacionId") UUID organizacionId);
}
