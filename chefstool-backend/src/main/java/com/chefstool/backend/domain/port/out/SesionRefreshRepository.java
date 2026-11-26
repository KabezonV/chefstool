package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.SesionRefresh;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SesionRefreshRepository extends JpaRepository<SesionRefresh, UUID> {
    Optional<SesionRefresh> findByTokenHashAndRevocadoAtIsNull(String tokenHash);
    void deleteByUsuarioId(UUID usuarioId);
}
