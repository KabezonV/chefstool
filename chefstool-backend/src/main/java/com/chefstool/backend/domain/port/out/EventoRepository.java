package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Evento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    List<Evento> findByOrganizacionIdAndDeletedAtIsNullOrderByFechaDesc(UUID organizacionId);
}
