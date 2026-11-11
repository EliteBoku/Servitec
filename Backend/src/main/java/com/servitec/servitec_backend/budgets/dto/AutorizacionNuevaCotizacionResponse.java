package com.servitec.servitec_backend.budgets.dto;

import java.time.LocalDateTime;

public record AutorizacionNuevaCotizacionResponse(
        Integer idAutorizacion,
        Integer idOrden,
        String codigoOrden,
        Integer idPresupuestoRechazado,
        Integer idEvaluacion,
        String justificacionEvaluacionTecnica,
        Integer idAdministrador,
        String nombreAdministrador,
        LocalDateTime fechaHoraAutorizacion,
        boolean utilizadaEnNuevaPropuesta
) {
}