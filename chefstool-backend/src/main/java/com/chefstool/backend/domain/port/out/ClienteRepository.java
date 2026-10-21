package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Cliente;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    List<Cliente> findByOrganizacionIdAndDeletedAtIsNullOrderByNombreAsc(UUID organizacionId);

    boolean existsByOrganizacionIdAndNombreIgnoreCaseAndDeletedAtIsNull(
            UUID organizacionId, String nombre);
}
