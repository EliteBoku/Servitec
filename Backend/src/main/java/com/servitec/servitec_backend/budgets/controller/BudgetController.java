package com.servitec.servitec_backend.budgets.controller;

import com.servitec.servitec_backend.budgets.dto.AutorizacionNuevaCotizacionResponse;
import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoComplementarioRequest;
import com.servitec.servitec_backend.budgets.dto.CrearPresupuestoOriginalRequest;
import com.servitec.servitec_backend.budgets.dto.EvaluacionTecnicaResponse;
import com.servitec.servitec_backend.budgets.dto.PresupuestoResponse;
import com.servitec.servitec_backend.budgets.dto.RegistrarDecisionPresupuestoRequest;
import com.servitec.servitec_backend.budgets.dto.RegistrarEvaluacionTecnicaRequest;
import com.servitec.servitec_backend.budgets.service.BudgetCreationService;
import com.servitec.servitec_backend.budgets.service.BudgetDecisionService;
import com.servitec.servitec_backend.budgets.service.RequoteAuthorizationService;
import com.servitec.servitec_backend.budgets.service.TechnicalEvaluationService;
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
    private final TechnicalEvaluationService technicalEvaluationService;
    private final RequoteAuthorizationService requoteAuthorizationService;
    private final CurrentUserProvider currentUserProvider;

    public BudgetController(
            BudgetCreationService budgetCreationService,
            BudgetDecisionService budgetDecisionService,
            TechnicalEvaluationService technicalEvaluationService,
            RequoteAuthorizationService requoteAuthorizationService,
            CurrentUserProvider currentUserProvider) {
        this.budgetCreationService = budgetCreationService;
        this.budgetDecisionService = budgetDecisionService;
        this.technicalEvaluationService = technicalEvaluationService;
        this.requoteAuthorizationService = requoteAuthorizationService;
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

    @PostMapping("/orders/{idOrden}/budgets/complementary")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PresupuestoResponse>> crearPresupuestoComplementario(
            @PathVariable Integer idOrden,
            @Valid @RequestBody CrearPresupuestoComplementarioRequest request) {

        exigirRol(RolNombre.TECNICO_ESPECIALISTA, "Solo el Técnico especialista asignado puede registrar repuestos adicionales.");
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        PresupuestoResponse creado = budgetCreationService.crearPresupuestoComplementario(idOrden, request, idTecnico);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Presupuesto complementario generado correctamente.", creado));
    }

    @PostMapping("/orders/{idOrden}/budgets/complementary/authorized/{idAutorizacion}")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<PresupuestoResponse>> crearNuevaCotizacionComplementaria(
            @PathVariable Integer idOrden,
            @PathVariable Integer idAutorizacion,
            @Valid @RequestBody CrearPresupuestoComplementarioRequest request) {

        exigirRol(RolNombre.TECNICO_ESPECIALISTA, "Solo el Técnico especialista asignado puede elaborar la nueva propuesta autorizada.");
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        PresupuestoResponse creado = budgetCreationService.crearNuevaCotizacionComplementaria(
                idOrden, idAutorizacion, request, idTecnico
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Nueva propuesta de presupuesto complementario generada correctamente.", creado));
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

    @PostMapping("/budgets/{idPresupuesto}/technical-evaluation")
    @PreAuthorize("hasRole('TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<EvaluacionTecnicaResponse>> registrarEvaluacionTecnica(
            @PathVariable Integer idPresupuesto,
            @Valid @RequestBody RegistrarEvaluacionTecnicaRequest request) {

        exigirRol(RolNombre.TECNICO_ESPECIALISTA, "Solo el Técnico especialista asignado puede realizar la evaluación técnica.");
        Integer idTecnico = currentUserProvider.getCurrentUserId();
        EvaluacionTecnicaResponse evaluacion =
                technicalEvaluationService.registrarEvaluacionTecnica(idPresupuesto, request, idTecnico);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Evaluación técnica registrada correctamente.", evaluacion));
    }

    @GetMapping("/orders/{idOrden}/technical-evaluations")
    @PreAuthorize("hasAnyRole('TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<EvaluacionTecnicaResponse>>> listarEvaluacionesPorOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        List<EvaluacionTecnicaResponse> evaluaciones =
                technicalEvaluationService.listarEvaluacionesPorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(evaluaciones));
    }

    @PostMapping("/budgets/{idPresupuesto}/requote-authorization")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<AutorizacionNuevaCotizacionResponse>> autorizarNuevaCotizacion(
            @PathVariable Integer idPresupuesto) {

        exigirRol(RolNombre.ADMINISTRADOR, "Solo el Administrador puede autorizar la elaboración de una nueva cotización complementaria.");
        Integer idAdmin = currentUserProvider.getCurrentUserId();
        AutorizacionNuevaCotizacionResponse autorizacion =
                requoteAuthorizationService.autorizarNuevaCotizacion(idPresupuesto, idAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Autorización para nueva cotización registrada correctamente.", autorizacion));
    }

    @GetMapping("/orders/{idOrden}/requote-authorizations")
    @PreAuthorize("hasAnyRole('TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<AutorizacionNuevaCotizacionResponse>>> listarAutorizacionesPorOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        List<AutorizacionNuevaCotizacionResponse> autorizaciones =
                requoteAuthorizationService.listarAutorizacionesPorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(autorizaciones));
    }

    private void exigirRol(RolNombre rolEsperado, String mensajeError) {
        String rolActual = currentUserProvider.getCurrentUserRole();
        if (!rolEsperado.name().equals(rolActual)) {
            throw new ForbiddenAccessException(mensajeError);
        }
    }
}