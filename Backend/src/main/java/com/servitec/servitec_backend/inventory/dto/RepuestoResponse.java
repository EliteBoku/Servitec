package com.servitec.servitec_backend.inventory.dto;

import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RepuestoResponse(
        Integer idRepuesto,
        String codigoRepuesto,
        String nombre,
        TipoItemCatalogo tipoItem,
        String unidadMedida,
        String descripcion,
        String imagenUrl,
        BigDecimal precioUnitarioActual,
        BigDecimal existenciasFisicas,
        BigDecimal cantidadesReservadas,
        BigDecimal existenciasDisponibles,
        Boolean activo,
        LocalDateTime fechaActualizacion
) {
}