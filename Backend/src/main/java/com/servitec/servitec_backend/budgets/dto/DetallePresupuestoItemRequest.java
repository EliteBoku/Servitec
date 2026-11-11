package com.servitec.servitec_backend.budgets.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DetallePresupuestoItemRequest(
        @NotNull(message = "El identificador del repuesto o insumo es obligatorio.")
        Integer idRepuesto,

        @NotNull(message = "La cantidad solicitada es obligatoria.")
        @DecimalMin(value = "0.01", inclusive = true, message = "La cantidad solicitada debe ser mayor que cero.")
        BigDecimal cantidadAutorizada,

        @Size(max = 500, message = "La justificación no puede superar los 500 caracteres.")
        String justificacion
) {
}