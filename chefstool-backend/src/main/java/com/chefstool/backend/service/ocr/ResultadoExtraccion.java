package com.chefstool.backend.service.ocr;

import com.chefstool.backend.web.dto.ExtraccionDocumento;
import java.math.BigDecimal;

public record ResultadoExtraccion(ExtraccionDocumento extraccion, BigDecimal confianza) {}
