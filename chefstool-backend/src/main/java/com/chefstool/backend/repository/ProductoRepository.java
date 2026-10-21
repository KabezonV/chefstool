package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Producto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);
}
