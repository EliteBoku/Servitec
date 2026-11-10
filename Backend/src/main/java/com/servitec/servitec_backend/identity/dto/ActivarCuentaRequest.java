package com.servitec.servitec_backend.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActivarCuentaRequest(

        @NotBlank(message = "El token de activación es obligatorio.")
        @Size(max = 128, message = "El token tiene un formato inválido.")
        String token,

        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(
                min = 12,
                max = 72,
                message = "La contraseña debe tener entre 12 y 72 caracteres."
        )
        String contrasena,

        @NotBlank(message = "Debe confirmar la contraseña.")
        @Size(max = 72)
        String confirmarContrasena

) {
}