package com.servitec.servitec_backend.identity.dto;

import com.servitec.servitec_backend.identity.service.SessionManagementService;

public record AuthenticationResponse(

        String accessToken,

        String tokenType,

        long expiresInSeconds,

        Integer idUsuario,

        String nombreCompleto,

        String correo,

        String rol

) {

    public static AuthenticationResponse from(
            SessionManagementService.SessionTokens tokens
    ) {
        return new AuthenticationResponse(
                tokens.accessToken(),
                "Bearer",
                tokens.accessTokenExpiresInSeconds(),
                tokens.idUsuario(),
                tokens.nombreCompleto(),
                tokens.correo(),
                tokens.rol()
        );
    }
}
