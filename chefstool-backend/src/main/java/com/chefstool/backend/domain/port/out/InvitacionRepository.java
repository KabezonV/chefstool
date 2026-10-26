package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.Invitacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvitacionRepository extends JpaRepository<Invitacion, UUID> {

    List<Invitacion> findByOrganizacionIdAndAceptadaAtIsNullOrderByCreatedAtAsc(
            UUID organizacionId);

    List<Invitacion> findByEmailIgnoreCaseAndAceptadaAtIsNullOrderByCreatedAtAsc(String email);

    boolean existsByOrganizacionIdAndEmailIgnoreCaseAndAceptadaAtIsNull(
            UUID organizacionId, String email);

    List<Invitacion> findByInvitadoPor(UUID usuarioId);
}
