package com.servitec.servitec_backend.identity.dto;

import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.RolNombre;

public record UsuarioCreadoResponse(

        Integer idUsuario,
        String nombreCompleto,
        String correo,
        RolNombre rol,
        EstadoCuenta estadoCuenta,
        String mensaje

) {
}