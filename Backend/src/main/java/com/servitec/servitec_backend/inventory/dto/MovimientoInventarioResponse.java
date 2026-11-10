package com.servitec.servitec_backend.inventory.dto;

import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimientoInventarioResponse(
        Integer idMovimiento,
        Integer idRepuesto,
        String codigoRepuesto,
        String nombreRepuesto,
        String unidadMedida,
        TipoMovimientoInventario tipoMovimiento,
        OrigenMovimientoInventario origenMovimiento,
        BigDecimal cantidadMovimiento,
        BigDecimal cantidadContada,
        BigDecimal existenciasAnteriores,
        BigDecimal existenciasResultantes,
        BigDecimal cantidadesReservadasActuales,
        BigDecimal existenciasDisponiblesActuales,
        String motivo,
        LocalDateTime fechaHoraMovimiento,
        Integer idUsuarioResponsable,
        String nombreUsuarioResponsable,
        Integer idDetallePresupuesto
) {
}