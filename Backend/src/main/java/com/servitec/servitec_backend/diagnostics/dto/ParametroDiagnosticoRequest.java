package com.servitec.servitec_backend.diagnostics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParametroDiagnosticoRequest(
        @NotBlank(message = "El nombre del parámetro de medición es obligatorio.")
        @Size(max = 80, message = "El nombre del parámetro no puede superar los 80 caracteres.")
        String nombreParametro,

        @NotBlank(message = "El valor del parámetro es obligatorio.")
        @Size(max = 150, message = "El valor del parámetro no puede superar los 150 caracteres.")
        String valor,

        @Size(max = 30, message = "La unidad de medida no puede superar los 30 caracteres.")
        String unidadMedida
) {
}