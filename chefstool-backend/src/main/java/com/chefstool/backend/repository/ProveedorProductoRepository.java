package com.chefstool.backend.repository;

import com.chefstool.backend.domain.ProveedorProducto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorProductoRepository extends JpaRepository<ProveedorProducto, UUID> {

    List<ProveedorProducto> findByOrganizacionId(UUID organizacionId);

    Optional<ProveedorProducto> findByProveedorIdAndProductoId(UUID proveedorId, UUID productoId);
}
