package com.servitec.servitec_backend.budgets.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ItemComplementarioRequest(
        @NotNull(message = "El identificador del repuesto o insumo adicional es obligatorio.")
        Integer idRepuesto,

        @NotNull(message = "La cantidad requerida es obligatoria.")
        @DecimalMin(value = "0.01", inclusive = true, message = "La cantidad requerida debe ser mayor que cero.")
        BigDecimal cantidadAutorizada,

        @NotBlank(message = "La justificación de la necesidad del elemento adicional es obligatoria.")
        @Size(max = 500, message = "La justificación no puede superar los 500 caracteres.")
        String justificacion
) {
}