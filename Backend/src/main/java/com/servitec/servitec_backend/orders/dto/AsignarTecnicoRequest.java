package com.servitec.servitec_backend.orders.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record AsignarTecnicoRequest(
        @NotNull(message = "Selecciona un técnico especialista disponible.")
        Integer idTecnicoAsignado,

        @NotNull(message = "Establece una fecha estimada de atención.")
        LocalDateTime fechaEstimada
) {
}