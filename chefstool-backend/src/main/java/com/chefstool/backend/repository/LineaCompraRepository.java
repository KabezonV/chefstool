package com.chefstool.backend.repository;

import com.chefstool.backend.domain.LineaCompra;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LineaCompraRepository extends JpaRepository<LineaCompra, UUID> {

    List<LineaCompra> findByCompraId(UUID compraId);
}
