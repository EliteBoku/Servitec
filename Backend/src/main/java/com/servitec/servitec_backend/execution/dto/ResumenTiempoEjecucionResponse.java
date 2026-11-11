package com.servitec.servitec_backend.execution.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.math.BigDecimal;
import java.util.List;

public record ResumenTiempoEjecucionResponse(
        Integer idOrden,
        String codigoOrden,
        EstadoOperativoOrden estadoOperativo,
        Long segundosEfectivosAcumulados,
        String tiempoEfectivoFormateado,
        BigDecimal horasEstimadas,
        BigDecimal horasEfectivas,
        BigDecimal diferenciaHoras,
        BigDecimal tarifaHoraAprobada,
        BigDecimal valorManoObraEstimado,
        BigDecimal valorManoObraCalculado,
        List<IntervaloTiempoResponse> intervalos
) {
}