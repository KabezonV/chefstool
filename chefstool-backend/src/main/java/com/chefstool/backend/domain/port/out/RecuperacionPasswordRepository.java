package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.RecuperacionPassword;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacionPasswordRepository extends JpaRepository<RecuperacionPassword, UUID> {

    Optional<RecuperacionPassword> findByTokenHash(String tokenHash);

    List<RecuperacionPassword> findByUsuarioIdAndUsadoAtIsNull(UUID usuarioId);
}
