package com.chefstool.backend.domain.model;

import com.chefstool.backend.domain.model.ExtraccionDocumento;
import java.math.BigDecimal;

public record ResultadoExtraccion(ExtraccionDocumento extraccion, BigDecimal confianza) {}
