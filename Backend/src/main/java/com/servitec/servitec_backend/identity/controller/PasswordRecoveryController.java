package com.servitec.servitec_backend.identity.controller;

import com.servitec.servitec_backend.identity.dto.RestablecerContrasenaRequest;
import com.servitec.servitec_backend.identity.dto.RestablecerContrasenaResponse;
import com.servitec.servitec_backend.identity.dto.SolicitudRecuperacionRequest;
import com.servitec.servitec_backend.identity.dto.SolicitudRecuperacionResponse;

import com.servitec.servitec_backend.identity.service.PasswordRecoveryService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/password/recovery")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    public PasswordRecoveryController(
            PasswordRecoveryService passwordRecoveryService
    ) {
        this.passwordRecoveryService = passwordRecoveryService;
    }

    /**
     * CV-11:
     * Solicita el envío del enlace de recuperación.
     *
     * Es público porque el usuario puede haber olvidado su contraseña.
     */
    @PostMapping("/request")
    public ResponseEntity<SolicitudRecuperacionResponse> solicitarRecuperacion(
            @Valid @RequestBody SolicitudRecuperacionRequest request
    ) {

        SolicitudRecuperacionResponse response =
                passwordRecoveryService.solicitarRecuperacion(request);

        return ResponseEntity.ok(response);
    }

    /**
     * CV-11:
     * Establece una nueva contraseña usando el token recibido por correo.
     */
    @PostMapping("/confirm")
    public ResponseEntity<RestablecerContrasenaResponse> restablecerContrasena(
            @Valid @RequestBody RestablecerContrasenaRequest request
    ) {

        RestablecerContrasenaResponse response =
                passwordRecoveryService.restablecerContrasena(request);

        return ResponseEntity.ok(response);
    }
}