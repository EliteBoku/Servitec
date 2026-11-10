package com.servitec.servitec_backend.inventory.dto;

import java.math.BigDecimal;

public record SolicitudReservaItem(
        Integer idDetallePresupuesto,
        Integer idRepuesto,
        BigDecimal cantidadAutorizada
) {
}