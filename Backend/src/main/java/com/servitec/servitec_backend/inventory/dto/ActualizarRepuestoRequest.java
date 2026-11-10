package com.servitec.servitec_backend.inventory.dto;

import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarRepuestoRequest(
        @NotBlank(message = "El código del repuesto o insumo es obligatorio.")
        @Size(max = 50, message = "El código no puede superar los 50 caracteres.")
        String codigoRepuesto,

        @NotBlank(message = "El nombre del elemento es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres.")
        String nombre,

        @NotNull(message = "El tipo de elemento (REPUESTO o INSUMO) es obligatorio.")
        TipoItemCatalogo tipoItem,

        @NotBlank(message = "La unidad de medida es obligatoria.")
        @Size(max = 30, message = "La unidad de medida no puede superar los 30 caracteres.")
        String unidadMedida,

        @NotBlank(message = "La descripción del elemento es obligatoria.")
        String descripcion,

        @Size(max = 500, message = "La URL o ruta de la imagen no puede superar los 500 caracteres.")
        String imagenUrl,

        @NotNull(message = "El precio unitario base es obligatorio.")
        @DecimalMin(value = "0.00", inclusive = true, message = "El precio unitario base no puede ser negativo.")
        BigDecimal precioUnitarioActual
) {
}