package com.servitec.servitec_backend.closures.controller;

import com.servitec.servitec_backend.closures.dto.CancelarOrdenRequest;
import com.servitec.servitec_backend.closures.dto.LiquidacionCancelacionResponse;
import com.servitec.servitec_backend.closures.service.OrderCancellationService;
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

@RestController
@RequestMapping("/api/v1/orders/{idOrden}/cancellation")
public class OrderCancellationController {

    private final OrderCancellationService orderCancellationService;
    private final CurrentUserProvider currentUserProvider;

    public OrderCancellationController(
            OrderCancellationService orderCancellationService,
            CurrentUserProvider currentUserProvider) {
        this.orderCancellationService = orderCancellationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<LiquidacionCancelacionResponse>> cancelarOrdenPausada(
            @PathVariable Integer idOrden,
            @Valid @RequestBody CancelarOrdenRequest request) {

        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.ADMINISTRADOR.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Administrador está autorizado para cancelar una orden de servicio.");
        }

        Integer idAdmin = currentUserProvider.getCurrentUserId();
        LiquidacionCancelacionResponse liquidacion =
                orderCancellationService.cancelarOrdenPausada(idOrden, request, idAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Orden de servicio cancelada y liquidación económica generada correctamente.", liquidacion));
    }

    @GetMapping("/settlement")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<LiquidacionCancelacionResponse>> consultarLiquidacionPorOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        LiquidacionCancelacionResponse liquidacion =
                orderCancellationService.consultarLiquidacionPorOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(liquidacion));
    }
}