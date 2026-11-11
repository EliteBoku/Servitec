package com.servitec.servitec_backend.diagnostics.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistrarDiagnosticoRequest(
        @NotBlank(message = "Registra las observaciones de la falla encontrada.")
        @Size(max = 800, message = "Las observaciones no pueden superar los 800 caracteres.")
        String observaciones,

        @NotEmpty(message = "Faltan parámetros obligatorios para esta categoría de equipo")
        List<@Valid ParametroDiagnosticoRequest> parametros
) {
}