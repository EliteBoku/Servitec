package com.servitec.servitec_backend.budgets.dto;

import com.servitec.servitec_backend.shared.enums.TipoDecisionPresupuesto;

import java.time.LocalDateTime;

public record DecisionPresupuestoResponse(
        Integer idDecision,
        TipoDecisionPresupuesto decision,
        Integer idClienteDecisor,
        String nombreClienteDecisor,
        LocalDateTime fechaHoraDecision
) {
}
