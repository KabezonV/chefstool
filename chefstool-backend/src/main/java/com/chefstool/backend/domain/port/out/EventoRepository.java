package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Evento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    List<Evento> findByOrganizacionIdAndDeletedAtIsNullOrderByFechaDesc(UUID organizacionId);

    org.springframework.data.domain.Page<Evento> findByOrganizacionIdAndDeletedAtIsNull(
            UUID organizacionId, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Evento>
            findByOrganizacionIdAndDeletedAtIsNullAndNombreContainingIgnoreCase(
                    UUID organizacionId, String nombre, org.springframework.data.domain.Pageable pageable);
}
