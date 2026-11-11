package com.servitec.servitec_backend.budgets.dto;

import java.math.BigDecimal;

public record DetallePresupuestoResponse(
        Integer idDetalle,
        Integer idRepuesto,
        String codigoRepuesto,
        String nombreRepuesto,
        String unidadMedida,
        BigDecimal cantidadAutorizada,
        BigDecimal precioUnitarioConservado,
        BigDecimal importeCalculado,
        BigDecimal cantidadUtilizada,
        String justificacion
) {
}