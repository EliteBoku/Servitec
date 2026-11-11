package com.servitec.servitec_backend.budgets.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CrearPresupuestoComplementarioRequest(
        @NotEmpty(message = "Debes seleccionar al menos un repuesto o insumo adicional del catálogo activo.")
        List<@Valid ItemComplementarioRequest> detalles
) {
}