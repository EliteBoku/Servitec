package com.servitec.servitec_backend.identity.controller;

import com.servitec.servitec_backend.identity.dto.AuthenticationResponse;
import com.servitec.servitec_backend.identity.dto.LoginRequest;
import com.servitec.servitec_backend.identity.service.AuthService;
import com.servitec.servitec_backend.identity.service.SessionManagementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME =
            "SERVITEC_REFRESH";

    private static final String REFRESH_COOKIE_PATH =
            "/api/v1/auth";

    private final AuthService authService;

    @Value("${app.security.refresh-cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${app.security.refresh-cookie.same-site:Lax}")
    private String cookieSameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        SessionManagementService.SessionTokens tokens =
                authService.login(
                        request.correo(),
                        request.contrasena()
                );

        ResponseCookie refreshCookie = createRefreshCookie(
                tokens.refreshToken(),
                tokens.refreshTokenExpiresInSeconds()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(AuthenticationResponse.from(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthenticationResponse> refresh(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {

        Optional<SessionManagementService.SessionTokens> resultado =
                authService.refresh(refreshToken);

        if (resultado.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .header(
                            HttpHeaders.SET_COOKIE,
                            clearRefreshCookie().toString()
                    )
                    .build();
        }

        SessionManagementService.SessionTokens tokens =
                resultado.get();

        ResponseCookie refreshCookie = createRefreshCookie(
                tokens.refreshToken(),
                tokens.refreshTokenExpiresInSeconds()
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshCookie.toString()
                )
                .body(AuthenticationResponse.from(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(
                    name = REFRESH_COOKIE_NAME,
                    required = false
            ) String refreshToken
    ) {

        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        clearRefreshCookie().toString()
                )
                .build();
    }

    private ResponseCookie createRefreshCookie(
            String refreshToken,
            long expiresInSeconds
    ) {

        return ResponseCookie
                .from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ofSeconds(expiresInSeconds))
                .build();
    }

    private ResponseCookie clearRefreshCookie() {

        return ResponseCookie
                .from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(Duration.ZERO)
                .build();
    }
}
