package com.chefstool.backend.domain.port.out;

import com.chefstool.backend.domain.model.FichaLinea;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FichaLineaRepository extends JpaRepository<FichaLinea, UUID> {

    List<FichaLinea> findByFichaId(UUID fichaId);
}
