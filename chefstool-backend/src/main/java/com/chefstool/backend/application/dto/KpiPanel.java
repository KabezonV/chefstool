package com.chefstool.backend.application.dto;

import java.math.BigDecimal;

public record KpiPanel(
        BigDecimal gastoMes,
        BigDecimal gastoMesAnterior,
        BigDecimal variacionPct,
        long comprasMes,
        long documentosPendientes,
        long alertasPendientes) {}
