package com.servitec.servitec_backend.inventory.dto;

import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegistrarMovimientoManualRequest(
        @NotNull(message = "El identificador del repuesto o insumo es obligatorio.")
        Integer idRepuesto,

        @NotNull(message = "El tipo de movimiento (ENTRADA, SALIDA o AJUSTE) es obligatorio.")
        TipoMovimientoInventario tipoMovimiento,

        @NotNull(message = "La cantidad o conteo físico es obligatorio.")
        @DecimalMin(value = "0.00", inclusive = true, message = "La cantidad no puede ser negativa.")
        BigDecimal cantidad,

        @NotBlank(message = "El motivo o justificación del movimiento es obligatorio.")
        String motivo
) {
}