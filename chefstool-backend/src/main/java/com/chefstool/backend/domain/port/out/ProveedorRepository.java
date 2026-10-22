package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Proveedor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    List<Proveedor> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);

    org.springframework.data.domain.Page<Proveedor> findByOrganizacionIdAndDeletedAtIsNull(
            UUID organizacionId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Proveedor>
            findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                    UUID organizacionId, String nombre, org.springframework.data.domain.Pageable pageable);
}
