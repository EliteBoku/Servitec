package com.servitec.servitec_backend.closures.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CerrarServicioRequest(
        @NotBlank(message = "Faltan las pruebas de funcionamiento: describe las pruebas realizadas y sus resultados antes de finalizar.")
        String pruebasFuncionamiento,

        @NotBlank(message = "No se ingresaron recomendaciones: registra los cuidados y recomendaciones que recibirá el Cliente.")
        String recomendaciones,

        @NotBlank(message = "Condiciones de garantía incompletas: define el alcance requerido para la garantía.")
        String garantiaAlcance,

        @NotBlank(message = "Condiciones de garantía incompletas: define la vigencia requerida para la garantía.")
        @Size(max = 100, message = "La vigencia de la garantía no puede superar los 100 caracteres.")
        String garantiaVigencia,

        List<@Valid CantidadUtilizadaItemRequest> cantidadesUtilizadas
) {
}