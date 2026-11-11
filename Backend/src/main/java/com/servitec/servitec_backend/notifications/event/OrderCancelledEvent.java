package com.servitec.servitec_backend.notifications.event;

import java.math.BigDecimal;

public record OrderCancelledEvent(
        Integer idOrden,
        String codigoOrden,
        String nombreCliente,
        String correoCliente,
        String justificacionCancelacion,
        BigDecimal totalLiquidacionCalculado
) {
}