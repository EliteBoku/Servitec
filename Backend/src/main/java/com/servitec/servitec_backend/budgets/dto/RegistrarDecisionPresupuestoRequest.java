package com.servitec.servitec_backend.budgets.dto;

import com.servitec.servitec_backend.shared.enums.TipoDecisionPresupuesto;
import jakarta.validation.constraints.NotNull;

public record RegistrarDecisionPresupuestoRequest(
        @NotNull(message = "Debes indicar explícitamente si apruebas (APROBADO) o rechazas (RECHAZADO) el presupuesto.")
        TipoDecisionPresupuesto decision
) {
}