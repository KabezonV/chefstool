package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.LineaCompra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LineaCompraRepository extends JpaRepository<LineaCompra, UUID> {

    List<LineaCompra> findByCompraId(UUID compraId);

    @Query(
            """
            select l from LineaCompra l
            join fetch l.compra c
            left join fetch c.proveedor
            where l.producto.id = :productoId
            order by c.fecha asc
            """)
    List<LineaCompra> historialPorProducto(@Param("productoId") UUID productoId);

    @Query(
            """
            select l from LineaCompra l
            join fetch l.compra c
            left join fetch c.proveedor
            left join fetch l.producto
            where c.organizacionId = :organizacionId
            order by c.fecha asc
            """)
    List<LineaCompra> lineasDeOrganizacion(@Param("organizacionId") UUID organizacionId);
}
