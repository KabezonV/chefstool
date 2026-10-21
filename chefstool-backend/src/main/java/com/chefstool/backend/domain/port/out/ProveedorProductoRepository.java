package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.ProveedorProducto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorProductoRepository extends JpaRepository<ProveedorProducto, UUID> {

    List<ProveedorProducto> findByOrganizacionId(UUID organizacionId);

    Optional<ProveedorProducto> findByProveedorIdAndProductoId(UUID proveedorId, UUID productoId);
}
