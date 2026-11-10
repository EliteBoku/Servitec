package com.servitec.servitec_backend.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearOrdenServicioRequest(
        @NotNull(message = "Selecciona la categoría del equipo.")
        Integer idCategoria,

        @NotBlank(message = "Describe brevemente el problema del equipo.")
        @Size(max = 500, message = "La descripción del problema no puede superar los 500 caracteres.")
        String descripcionProblema,

        @NotBlank(message = "Ingresa la dirección donde se encuentra el equipo.")
        @Size(max = 255, message = "La dirección del servicio no puede superar los 255 caracteres.")
        String direccionServicio,

        Boolean autorizaTratamientoDatos
) {
}