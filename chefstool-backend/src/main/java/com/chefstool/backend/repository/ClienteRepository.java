package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Cliente;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);
}
