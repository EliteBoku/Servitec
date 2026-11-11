package com.servitec.servitec_backend.notifications.event;

import java.math.BigDecimal;

public record ComplementaryBudgetCreatedEvent(
        Integer idPresupuesto,
        String codigoPresupuesto,
        Integer idOrden,
        String codigoOrden,
        String nombreCliente,
        String correoCliente,
        BigDecimal totalAdicional,
        boolean esNuevaPropuestaAutorizada
) {
}