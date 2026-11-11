package com.servitec.servitec_backend.execution.controller;

import com.servitec.servitec_backend.execution.dto.PausarReparacionRequest;
import com.servitec.servitec_backend.execution.dto.ResumenTiempoEjecucionResponse;
import com.servitec.servitec_backend.execution.service.ExecutionTimeService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders/{idOrden}/execution")
public class ExecutionController {

    private final ExecutionTimeService executionTimeService;
    private final CurrentUserProvider currentUserProvider;

    public ExecutionController(
            ExecutionTimeService executionTimeService,
            CurrentUserProvider currentUserProvider) {
        this.executionTimeService = executionTimeService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/start")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<ResumenTiempoEjecucionResponse>> iniciarReparacion(
            @PathVariable Integer idOrden) {

        exigirRolTecnico();
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        ResumenTiempoEjecucionResponse resultado = executionTimeService.iniciarReparacion(idOrden, idTecnico);

        return ResponseEntity.ok(ApiResponse.ok("Reparación iniciada correctamente.", resultado));
    }

    @PostMapping("/pause")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<ResumenTiempoEjecucionResponse>> pausarReparacion(
            @PathVariable Integer idOrden,
            @Valid @RequestBody PausarReparacionRequest request) {

        exigirRolTecnico();
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        ResumenTiempoEjecucionResponse resultado = executionTimeService.pausarReparacion(idOrden, request, idTecnico);

        return ResponseEntity.ok(ApiResponse.ok("Reparación pausada correctamente.", resultado));
    }

    @PostMapping("/resume")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<ResumenTiempoEjecucionResponse>> reanudarReparacion(
            @PathVariable Integer idOrden) {

        exigirRolTecnico();
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        ResumenTiempoEjecucionResponse resultado = executionTimeService.reanudarReparacion(idOrden, idTecnico);

        return ResponseEntity.ok(ApiResponse.ok("Reparación reanudada correctamente.", resultado));
    }

    @GetMapping("/time-summary")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<ResumenTiempoEjecucionResponse>> consultarResumenTiempo(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        ResumenTiempoEjecucionResponse resumen =
                executionTimeService.consultarResumenTiempo(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(resumen));
    }

    private void exigirRolTecnico() {
        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.TECNICO_ESPECIALISTA.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Técnico especialista asignado puede ejecutar, pausar o reanudar la reparación.");
        }
    }
}