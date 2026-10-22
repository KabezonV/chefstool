package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Compra;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompraRepository extends JpaRepository<Compra, UUID> {

    List<Compra> findByOrganizacionIdOrderByFechaDesc(UUID organizacionId);

    @Query(
            """
            select c from Compra c
            left join fetch c.proveedor
            left join fetch c.documento
            where c.organizacionId = :organizacionId
              and (:proveedorId is null or c.proveedor.id = :proveedorId)
              and (:desde is null or c.fecha >= :desde)
              and (:hasta is null or c.fecha <= :hasta)
            order by c.fecha desc, c.createdAt desc
            """)
    List<Compra> buscar(
            @Param("organizacionId") UUID organizacionId,
            @Param("proveedorId") UUID proveedorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /** Variante paginada: sin fetch joins para que la paginacion ocurra en la base de datos. */
    @Query(
            """
            select c from Compra c
            where c.organizacionId = :organizacionId
              and (:proveedorId is null or c.proveedor.id = :proveedorId)
              and (:desde is null or c.fecha >= :desde)
              and (:hasta is null or c.fecha <= :hasta)
            """)
    Page<Compra> buscarPaginado(
            @Param("organizacionId") UUID organizacionId,
            @Param("proveedorId") UUID proveedorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            Pageable pageable);
}
