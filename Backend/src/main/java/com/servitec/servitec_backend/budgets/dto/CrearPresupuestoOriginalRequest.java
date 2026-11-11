package com.servitec.servitec_backend.budgets.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CrearPresupuestoOriginalRequest(
        @NotNull(message = "La tarifa por hora de mano de obra es obligatoria.")
        @DecimalMin(value = "0.01", inclusive = true, message = "La tarifa por hora debe ser mayor que cero.")
        BigDecimal tarifaHora,

        @NotNull(message = "Las horas estimadas de trabajo son obligatorias.")
        @DecimalMin(value = "0.01", inclusive = true, message = "Las horas estimadas deben ser mayores que cero.")
        BigDecimal horasEstimadas,

        List<@Valid DetallePresupuestoItemRequest> detalles
) {
}