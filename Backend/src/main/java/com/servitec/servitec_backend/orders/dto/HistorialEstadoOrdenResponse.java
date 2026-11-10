package com.servitec.servitec_backend.orders.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.time.LocalDateTime;

public record HistorialEstadoOrdenResponse(
        Integer idHistorialEstado,
        Integer idOrden,
        EstadoOperativoOrden estadoAnterior,
        EstadoOperativoOrden estadoNuevo,
        String justificacion,
        LocalDateTime fechaHoraCambio,
        Integer idUsuarioResponsable,
        String nombreUsuarioResponsable,
        Integer idDecisionPresupuesto
) {
}