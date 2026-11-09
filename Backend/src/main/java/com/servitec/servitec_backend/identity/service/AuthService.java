package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.security.SecurityUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SessionManagementService sessionManagementService;

    public AuthService(
            AuthenticationManager authenticationManager,
            SessionManagementService sessionManagementService
    ) {
        this.authenticationManager = authenticationManager;
        this.sessionManagementService = sessionManagementService;
    }

    public SessionManagementService.SessionTokens login(
            String correo,
            String contrasena
    ) {

        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            correo.trim(),
                            contrasena
                    )
            );

        } catch (AuthenticationException exception) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credenciales inválidas. Por favor, verifique sus datos."
            );
        }

        SecurityUser principal =
                (SecurityUser) authentication.getPrincipal();

        return sessionManagementService.createSession(
                principal.getIdUsuario()
        );
    }

    public java.util.Optional<SessionManagementService.SessionTokens> refresh(
            String refreshToken
    ) {
        return sessionManagementService.refresh(refreshToken);
    }

    public void logout(String refreshToken) {
        sessionManagementService.revoke(refreshToken);
    }
}
