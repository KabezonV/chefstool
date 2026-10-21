package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Proveedor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    List<Proveedor> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);
}
