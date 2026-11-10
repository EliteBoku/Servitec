package com.servitec.servitec_backend.inventory.controller;

import com.servitec.servitec_backend.inventory.dto.MovimientoInventarioResponse;
import com.servitec.servitec_backend.inventory.dto.RegistrarMovimientoManualRequest;
import com.servitec.servitec_backend.inventory.service.InventoryMovementService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/movements")
public class InventoryMovementController {

    private final InventoryMovementService inventoryMovementService;
    private final CurrentUserProvider currentUserProvider;

    public InventoryMovementController(
            InventoryMovementService inventoryMovementService,
            CurrentUserProvider currentUserProvider) {
        this.inventoryMovementService = inventoryMovementService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<MovimientoInventarioResponse>> registrarMovimientoManual(
            @Valid @RequestBody RegistrarMovimientoManualRequest request) {

        exigirRolAdministrador();
        Integer idAdmin = currentUserProvider.getCurrentUserId();
        MovimientoInventarioResponse registrado = inventoryMovementService.registrarMovimientoManual(request, idAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Movimiento de inventario registrado correctamente.", registrado));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<MovimientoInventarioResponse>>> consultarHistorial(
            @RequestParam(required = false) Integer idRepuesto,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) OrigenMovimientoInventario origenMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta) {

        exigirRolAdministrador();
        List<MovimientoInventarioResponse> historial = inventoryMovementService.consultarHistorial(
                idRepuesto, tipoMovimiento, origenMovimiento, fechaDesde, fechaHasta
        );

        return ResponseEntity.ok(ApiResponse.ok(historial));
    }

    private void exigirRolAdministrador() {
        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.ADMINISTRADOR.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Administrador está autorizado para gestionar movimientos de inventario.");
        }
    }
}