package com.chefstool.backend.application.dto;

import java.math.BigDecimal;

/** Importe agrupado por proveedor o por mes. */
public record GastoAgrupado(String etiqueta, BigDecimal importe) {}
