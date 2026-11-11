package com.servitec.servitec_backend.budgets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegistrarEvaluacionTecnicaRequest(
        @NotNull(message = "Debes indicar si los elementos rechazados son indispensables para continuar la reparación.")
        Boolean elementosIndispensables,

        @NotBlank(message = "La justificación técnica de la evaluación es obligatoria.")
        @Size(max = 800, message = "La justificación técnica no puede superar los 800 caracteres.")
        String justificacion
) {
}