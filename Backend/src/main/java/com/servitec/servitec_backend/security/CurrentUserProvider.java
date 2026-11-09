package com.servitec.servitec_backend.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public Integer getCurrentUserId() {

        JwtAuthenticationToken authentication = getJwtAuthentication();

        try {

            Integer idUsuario = Integer.valueOf(
                    authentication.getToken().getSubject()
            );

            if (idUsuario <= 0) {
                throw new NumberFormatException();
            }

            return idUsuario;

        } catch (NumberFormatException exception) {

            throw new AuthenticationCredentialsNotFoundException(
                    "El identificador del usuario autenticado no es válido."
            );
        }
    }

    public String getCurrentUserRole() {

        return getJwtAuthentication()
                .getToken()
                .getClaimAsString("role");
    }

    private JwtAuthenticationToken getJwtAuthentication() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)
                || !authentication.isAuthenticated()) {

            throw new AuthenticationCredentialsNotFoundException(
                    "No existe un usuario autenticado en el contexto actual."
            );
        }

        return jwtAuth;
    }
}