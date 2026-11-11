package com.servitec.servitec_backend.closures.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CantidadUtilizadaItemRequest(
        @NotNull(message = "El identificador del detalle de presupuesto aprobado es obligatorio.")
        Integer idDetallePresupuesto,

        @NotNull(message = "La cantidad efectivamente utilizada es obligatoria.")
        @DecimalMin(value = "0.00", inclusive = true, message = "La cantidad utilizada no puede ser negativa.")
        BigDecimal cantidadUtilizada
) {
}