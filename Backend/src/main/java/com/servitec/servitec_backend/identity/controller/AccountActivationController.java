package com.servitec.servitec_backend.identity.controller;

import com.servitec.servitec_backend.identity.dto.ActivarCuentaRequest;
import com.servitec.servitec_backend.identity.dto.CuentaActivadaResponse;
import com.servitec.servitec_backend.identity.service.AccountActivationService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AccountActivationController {

    private final AccountActivationService accountActivationService;

    public AccountActivationController(
            AccountActivationService accountActivationService
    ) {
        this.accountActivationService = accountActivationService;
    }

    /**
     * CV-10:
     * Activa una cuenta mediante un token seguro.
     *
     * Endpoint público: no requiere JWT.
     */
    @PostMapping("/activate")
    public ResponseEntity<CuentaActivadaResponse> activarCuenta(
            @Valid @RequestBody ActivarCuentaRequest request
    ) {

        CuentaActivadaResponse response =
                accountActivationService.activarCuenta(request);

        return ResponseEntity.ok(response);
    }
}