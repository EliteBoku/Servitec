package com.servitec.servitec_backend.closures.dto;

import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LiquidacionCancelacionResponse(
        Integer idCancelacion,
        Integer idLiquidacion,
        Integer idOrden,
        String codigoOrden,
        EstadoOperativoOrden estadoOperativo,
        Integer idAdministrador,
        String nombreAdministrador,
        String justificacionCancelacion,
        LocalDateTime fechaHoraCancelacion,
        Long segundosEfectivos,
        String tiempoEfectivoFormateado,
        BigDecimal horasEstimadas,
        BigDecimal horasEfectivas,
        BigDecimal diferenciaHoras,
        BigDecimal tarifaHoraAplicada,
        BigDecimal valorManoObra,
        BigDecimal valorRepuestosInsumos,
        BigDecimal totalCalculado,
        LocalDateTime fechaGeneracion,
        String notaInformativa,
        List<ItemUtilizadoLiquidacionResponse> elementosUtilizados
) {
}