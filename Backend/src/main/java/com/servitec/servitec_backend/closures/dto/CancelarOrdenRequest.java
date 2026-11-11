package com.servitec.servitec_backend.closures.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CancelarOrdenRequest(
        @NotBlank(message = "Debes ingresar una justificación obligatoria sobre la decisión de cancelar el servicio.")
        @Size(max = 800, message = "La justificación no puede superar los 800 caracteres.")
        String justificacion,

        List<@Valid CantidadUtilizadaItemRequest> cantidadesUtilizadas
) {
}