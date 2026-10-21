package com.chefstool.backend.repository;

import com.chefstool.backend.domain.FichaLinea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaLineaRepository extends JpaRepository<FichaLinea, UUID> {

    List<FichaLinea> findByFichaId(UUID fichaId);
}
