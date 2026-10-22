package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Producto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {

    List<Producto> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);

    org.springframework.data.domain.Page<Producto> findByOrganizacionIdAndDeletedAtIsNull(
            UUID organizacionId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Producto>
            findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                    UUID organizacionId, String nombre, org.springframework.data.domain.Pageable pageable);
}
