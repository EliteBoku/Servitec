package com.servitec.servitec_backend.closures.dto;

import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;

import java.math.BigDecimal;

public record ItemUtilizadoLiquidacionResponse(
        Integer idDetallePresupuesto,
        Integer idPresupuesto,
        TipoPresupuesto tipoPresupuesto,
        Integer idRepuesto,
        String codigoRepuesto,
        String nombreRepuesto,
        String unidadMedida,
        BigDecimal cantidadAutorizada,
        BigDecimal cantidadUtilizada,
        BigDecimal cantidadLiberadaNoUtilizada,
        BigDecimal precioUnitarioConservado,
        BigDecimal importeUtilizado
) {
}