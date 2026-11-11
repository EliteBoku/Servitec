package com.servitec.servitec_backend.closures.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CierreTecnicoResponse(
        Integer idCierre,
        Integer idOrden,
        String codigoOrden,
        EstadoOperativoOrden estadoOperativo,
        Integer idTecnico,
        String nombreTecnico,
        String pruebasFuncionamiento,
        String recomendaciones,
        String garantiaAlcance,
        String garantiaVigencia,
        LocalDateTime fechaCierre,
        Long segundosEfectivosAcumulados,
        String tiempoEfectivoFormateado,
        BigDecimal horasEstimadas,
        BigDecimal horasEfectivas,
        BigDecimal diferenciaHoras,
        BigDecimal tarifaHoraAprobada,
        BigDecimal valorManoObraCalculado,
        BigDecimal valorRepuestosUtilizados,
        BigDecimal valorTotalCalculado,
        List<ItemUtilizadoLiquidacionResponse> elementosAutorizados
) {
}