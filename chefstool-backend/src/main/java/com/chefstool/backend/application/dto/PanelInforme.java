package com.chefstool.backend.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record PanelInforme(
        KpiPanel kpis,
        BigDecimal ahorroPotencial,
        int lineasMejorables,
        long comprasAnalizadas,
        BigDecimal gastoTotal,
        List<GastoAgrupado> gastoPorProveedor,
        List<GastoAgrupado> gastoPorMes,
        List<VariacionPrecio> mayoresVariaciones,
        List<AlertaResumen> alertas,
        List<DocumentoPendiente> documentosPorRevisar) {}
