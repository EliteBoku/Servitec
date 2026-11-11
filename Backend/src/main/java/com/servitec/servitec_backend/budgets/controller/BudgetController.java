package com.servitec.servitec_backend.budgets.controller;

import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoOriginalRequest;
import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.RegistrarDecisionPresupuestoRequest;
import com.servitec.servitec_backend.budgets.service.BudgetCreationService;
import com.servitec.servitec_backend.budgets.service.BudgetDecisionService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoDecisionPresupuesto;
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
@RequestMapping("/api/v1")
public class BudgetController {

    private final BudgetCreationService budgetCreationService;
    private final BudgetDecisionService budgetDecisionService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetController(
            BudgetCreationService budgetCreationService,
            BudgetDecisionService budgetDecisionService,
            CurrentUserProvider currentUserProvider) {
        this.budgetCreationService = budgetCreationService;
        this.budgetDecisionService = budgetDecisionService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/orders/{idOrden}/budgets/original")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PresupuestoResponse>> crearPresupuestoOriginal(
            @PathVariable Integer idOrden,
            @Valid @RequestBody CrearPresupuestoOriginalRequest request) {

        exigirRol(RolNombre.TECNICO_ESPECIALISTA, "Solo el Técnico especialista asignado puede elaborar el presupuesto.");
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        PresupuestoResponse creado = budgetCreationService.crearPresupuestoOriginal(idOrden, request, idTecnico);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Presupuesto generado correctamente.", creado));
    }

    @GetMapping("/orders/{idOrden}/budgets")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<PresupuestoResponse>>> listarPresupuestosPorOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        List<PresupuestoResponse> presupuestos =
                budgetCreationService.listarPresupuestosPorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(presupuestos));
    }

    @GetMapping("/budgets/{idPresupuesto}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<PresupuestoResponse>> consultarPresupuestoPorId(
            @PathVariable Integer idPresupuesto) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        PresupuestoResponse presupuesto =
                budgetCreationService.consultarPresupuestoPorId(idPresupuesto, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(presupuesto));
    }

    @PostMapping("/budgets/{idPresupuesto}/decision")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<PresupuestoResponse>> decidirSobrePresupuesto(
            @PathVariable Integer idPresupuesto,
            @Valid @RequestBody RegistrarDecisionPresupuestoRequest request) {

        exigirRol(RolNombre.CLIENTE, "Solo el Cliente propietario de la orden puede aprobar o rechazar el presupuesto.");
        Integer idCliente = currentUserProvider.getCurrentUserId();
        PresupuestoResponse resultado =
                budgetDecisionService.decidirSobrePresupuesto(idPresupuesto, request, idCliente);

        String mensaje = request.decision() == TipoDecisionPresupuesto.APROBADO
                ? "Presupuesto aprobado correctamente."
                : "Presupuesto rechazado correctamente.";

        return ResponseEntity.ok(ApiResponse.ok(mensaje, resultado));
    }

    private void exigirRol(RolNombre rolEsperado, String mensajeError) {
        String rolActual = currentUserProvider.getCurrentUserRole();
        if (!rolEsperado.name().equals(rolActual)) {
            throw new ForbiddenAccessException(mensajeError);
        }
    }
}