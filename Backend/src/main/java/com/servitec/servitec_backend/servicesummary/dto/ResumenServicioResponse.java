package com.servitec.servitec_backend.servicesummary.dto;

import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.closures.dto.ItemUtilizadoLiquidacionResponse;
import com.servitec.servitec_backend.diagnostics.dto.DiagnosticoResponse;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ResumenServicioResponse(
        // 1. Información general de la orden finalizada
        Integer idOrden,
        String codigoOrden,
        EstadoOperativoOrden estadoOperativo,
        boolean soloLectura,
        Integer idCategoria,
        String nombreCategoria,
        String direccionServicio,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaFinalizacion,
        Integer idCliente,
        String nombreCliente,
        Integer idTecnicoResponsable,
        String nombreTecnicoResponsable,

        // 2. Problema reportado y diagnóstico técnico inicial
        String problemaReportado,
        DiagnosticoResponse diagnosticoInicial,

        // 3. Historial de presupuestos (original aprobado y complementarios solicitados con sus decisiones)
        PresupuestoResponse presupuestoOriginalAprobado,
        List<PresupuestoResponse> presupuestosComplementarios,

        // 4. Tiempo efectivo de ejecución y mano de obra calculada (REQ-12, REQ-26, RN-11, RN-13)
        Long segundosEfectivosAcumulados,
        String tiempoEfectivoFormateado,
        BigDecimal horasEstimadas,
        BigDecimal horasEfectivas,
        BigDecimal diferenciaHoras,
        BigDecimal tarifaHoraAprobada,
        BigDecimal valorManoObraEstimado,
        BigDecimal valorManoObraCalculado,

        // 5. Repuestos e insumos aprobados y efectivamente utilizados (RN-14)
        List<ItemUtilizadoLiquidacionResponse> repuestosEInsumosEfectivamenteUtilizados,
        List<ItemUtilizadoLiquidacionResponse> desgloseReservasYCantidadesLiberadas,
        BigDecimal valorRepuestosEInsumosUtilizados,

        // 6. Valor económico total calculado del servicio
        BigDecimal valorTotalCalculadoServicio,
        String notaLegalEconomica,

        // 7. Información del cierre técnico y condiciones de garantía (REQ-16 y Sección 11.2)
        String pruebasFuncionamientoRealizadas,
        String recomendacionesCliente,
        String garantiaAlcance,
        String garantiaVigencia
) {
}