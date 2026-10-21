package com.chefstool.backend.repository;

import com.chefstool.backend.domain.Evento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    List<Evento> findByOrganizacionIdAndDeletedAtIsNullOrderByFechaDesc(UUID organizacionId);
}
