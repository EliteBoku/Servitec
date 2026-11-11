package com.servitec.servitec_backend.diagnostics.controller;

import com.servitec.servitec_backend.diagnostics.dto.DiagnosticoResponse;
import com.servitec.servitec_backend.diagnostics.dto.PlantillaParametroCategoriaResponse;
import com.servitec.servitec_backend.diagnostics.dto.RegistrarDiagnosticoRequest;
import com.servitec.servitec_backend.diagnostics.service.DiagnosticService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{idOrden}/diagnostic")
public class DiagnosticController {

    private final DiagnosticService diagnosticService;
    private final CurrentUserProvider currentUserProvider;

    public DiagnosticController(
            DiagnosticService diagnosticService,
            CurrentUserProvider currentUserProvider) {
        this.diagnosticService = diagnosticService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/template")
    @PreAuthorize("hasAnyRole('TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<PlantillaParametroCategoriaResponse>>> obtenerPlantillaDiagnostico(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        List<PlantillaParametroCategoriaResponse> plantilla =
                diagnosticService.obtenerPlantillaParaOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(plantilla));
    }

    @PostMapping
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<DiagnosticoResponse>> registrarDiagnostico(
            @PathVariable Integer idOrden,
            @Valid @RequestBody RegistrarDiagnosticoRequest request) {

        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.TECNICO_ESPECIALISTA.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Técnico especialista asignado puede registrar el diagnóstico.");
        }

        Integer idTecnico = currentUserProvider.getCurrentUserId();
        DiagnosticoResponse registrado = diagnosticService.registrarDiagnostico(idOrden, request, idTecnico);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Diagnóstico registrado correctamente.", registrado));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<DiagnosticoResponse>> consultarDiagnostico(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        DiagnosticoResponse diagnostico = diagnosticService.consultarDiagnosticoPorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(diagnostico));
    }
}