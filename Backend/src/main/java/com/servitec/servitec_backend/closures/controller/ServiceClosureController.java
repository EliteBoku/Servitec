package com.servitec.servitec_backend.closures.controller;

import com.servitec.servitec_backend.closures.dto.ActualizarCantidadesUtilizadasRequest;
import com.servitec.servitec_backend.closures.dto.CerrarServicioRequest;
import com.servitec.servitec_backend.closures.dto.CierreTecnicoResponse;
import com.servitec.servitec_backend.closures.dto.ItemUtilizadoLiquidacionResponse;
import com.servitec.servitec_backend.closures.service.ServiceClosureService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders/{idOrden}/closure")
public class ServiceClosureController {

    private final ServiceClosureService serviceClosureService;
    private final CurrentUserProvider currentUserProvider;

    public ServiceClosureController(
            ServiceClosureService serviceClosureService,
            CurrentUserProvider currentUserProvider) {
        this.serviceClosureService = serviceClosureService;
        this.currentUserProvider = currentUserProvider;
    }

    @PatchMapping("/used-quantities")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<ItemUtilizadoLiquidacionResponse>>> actualizarCantidadesUtilizadas(
            @PathVariable Integer idOrden,
            @Valid @RequestBody ActualizarCantidadesUtilizadasRequest request) {

        exigirRolTecnico();
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        List<ItemUtilizadoLiquidacionResponse> actualizados =
                serviceClosureService.actualizarCantidadesUtilizadas(idOrden, request, idTecnico);

        return ResponseEntity.ok(ApiResponse.ok("Cantidades utilizadas actualizadas correctamente.", actualizados));
    }

    @PostMapping
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<CierreTecnicoResponse>> cerrarServicioTecnico(
            @PathVariable Integer idOrden,
            @Valid @RequestBody CerrarServicioRequest request) {

        exigirRolTecnico();
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        CierreTecnicoResponse cierre = serviceClosureService.cerrarServicioTecnico(idOrden, request, idTecnico);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Servicio finalizado correctamente.", cierre));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<CierreTecnicoResponse>> consultarCierrePorOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        CierreTecnicoResponse cierre = serviceClosureService.consultarCierrePorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(cierre));
    }

    private void exigirRolTecnico() {
        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.TECNICO_ESPECIALISTA.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Técnico especialista asignado puede registrar el cierre técnico.");
        }
    }
}