package com.servitec.servitec_backend.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestablecerContrasenaRequest(

        @NotBlank(message = "El token de recuperación es obligatorio.")
        @Size(max = 128, message = "El token tiene un formato inválido.")
        String token,

        @NotBlank(message = "La nueva contraseña es obligatoria.")
        @Size(
                min = 12,
                max = 72,
                message = "La contraseña debe tener entre 12 y 72 caracteres."
        )
        String nuevaContrasena,

        @NotBlank(message = "La confirmación de contraseña es obligatoria.")
        @Size(max = 72)
        String confirmarContrasena

) {
}