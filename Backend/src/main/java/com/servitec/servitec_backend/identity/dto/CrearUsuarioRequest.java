package com.servitec.servitec_backend.identity.dto;

import com.servitec.servitec_backend.shared.enums.RolNombre;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(

        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres.")
        String nombreCompleto,

        @NotBlank(message = "El correo electrónico es obligatorio.")
        @Email(message = "El correo electrónico no tiene un formato válido.")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres.")
        String correo,

        @NotNull(message = "Debe seleccionar un rol.")
        RolNombre rol

) {
}