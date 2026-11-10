package com.servitec.servitec_backend.inventory.dto;

import java.math.BigDecimal;

public record ConsumoReservaItem(
        Integer idDetallePresupuesto,
        Integer idRepuesto,
        BigDecimal cantidadUtilizada
) {
}