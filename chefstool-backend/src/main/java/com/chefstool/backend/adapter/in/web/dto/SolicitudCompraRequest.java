package com.chefstool.backend.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SolicitudCompraRequest(
        @NotBlank @Size(max = 160) String concepto,
        @Size(max = 2000) String descripcion,
        @Size(max = 200) String proveedor,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal importe) {}
