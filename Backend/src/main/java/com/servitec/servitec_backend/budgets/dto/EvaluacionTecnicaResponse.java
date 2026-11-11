package com.servitec.servitec_backend.budgets.dto;

import java.time.LocalDateTime;

public record EvaluacionTecnicaResponse(
        Integer idEvaluacion,
        Integer idPresupuestoRechazado,
        Integer idOrden,
        String codigoOrden,
        Boolean elementosIndispensables,
        String justificacion,
        Integer idTecnico,
        String nombreTecnico,
        LocalDateTime fechaHora,
        boolean permiteReanudarReparacion
) {
}