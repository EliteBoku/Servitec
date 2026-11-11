package com.servitec.servitec_backend.budgets.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PresupuestoResponse(
        Integer idPresupuesto,
        String codigoPresupuesto,
        Integer idOrden,
        String codigoOrden,
        EstadoOperativoOrden estadoOperativoOrden,
        TipoPresupuesto tipoPresupuesto,
        EstadoPresupuesto estado,
        Integer idTecnicoCreador,
        String nombreTecnicoCreador,
        Integer idAutorizacion,
        BigDecimal tarifaHoraAprobada,
        BigDecimal horasEstimadas,
        BigDecimal valorManoObraEstimado,
        BigDecimal subtotalRepuestos,
        BigDecimal totalPresupuesto,
        String condicionManoObra,
        LocalDateTime fechaEmision,
        List<DetallePresupuestoResponse> detalles,
        DecisionPresupuestoResponse decisionRegistrada
) {
}