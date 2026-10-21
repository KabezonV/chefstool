package com.chefstool.backend.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PuntoPrecio(LocalDate fecha, BigDecimal precio, String proveedorNombre) {}
