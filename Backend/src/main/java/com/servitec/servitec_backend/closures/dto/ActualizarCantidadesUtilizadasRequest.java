package com.servitec.servitec_backend.closures.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ActualizarCantidadesUtilizadasRequest(
        @NotEmpty(message = "Debes enviar al menos un detalle para actualizar su cantidad utilizada.")
        List<@Valid CantidadUtilizadaItemRequest> cantidadesUtilizadas
) {
}