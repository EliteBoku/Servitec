package com.servitec.servitec_backend.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PausarReparacionRequest(
        @NotBlank(message = "Debes ingresar una justificación para pausar la reparación.")
        @Size(max = 800, message = "La justificación de la pausa no puede superar los 800 caracteres.")
        String justificacion
) {
}