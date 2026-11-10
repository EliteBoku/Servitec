package com.servitec.servitec_backend.orders.controller;

import com.servitec.servitec_backend.orders.dto.AsignarTecnicoRequest;
import com.servitec.servitec_backend.orders.dto.CategoriaEquipoResponse;
import com.servitec.servitec_backend.orders.dto.CrearOrdenServicioRequest;
import com.servitec.servitec_backend.orders.dto.EstadoConsentimientoResponse;
import com.servitec.servitec_backend.orders.dto.HistorialEstadoOrdenResponse;
import com.servitec.servitec_backend.orders.dto.OrdenServicioResponse;
import com.servitec.servitec_backend.orders.service.OrderAssignmentService;
import com.servitec.servitec_backend.orders.service.OrderService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderAssignmentService orderAssignmentService;
    private final CurrentUserProvider currentUserProvider;

    public OrderController(
            OrderService orderService,
            OrderAssignmentService orderAssignmentService,
            CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.orderAssignmentService = orderAssignmentService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<CategoriaEquipoResponse>>> listarCategoriasActivas() {
        List<CategoriaEquipoResponse> categorias = orderService.listarCategoriasActivas();
        return ResponseEntity.ok(ApiResponse.ok(categorias));
    }

    @GetMapping("/consent-status")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<EstadoConsentimientoResponse>> consultarEstadoConsentimiento() {
        exigirRol(RolNombre.CLIENTE, "Solo los clientes pueden consultar el estado de consentimiento de datos.");
        Integer idCliente = currentUserProvider.getCurrentUserId();
        EstadoConsentimientoResponse estado = orderService.consultarEstadoConsentimiento(idCliente);
        return ResponseEntity.ok(ApiResponse.ok(estado));
    }

    @PostMapping
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ApiResponse<OrdenServicioResponse>> crearSolicitud(
            @Valid @RequestBody CrearOrdenServicioRequest request) {

        exigirRol(RolNombre.CLIENTE, "Solo los usuarios con rol Cliente pueden registrar solicitudes de servicio.");
        Integer idCliente = currentUserProvider.getCurrentUserId();
        OrdenServicioResponse creada = orderService.crearSolicitudServicio(request, idCliente);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Solicitud registrada correctamente.", creada));
    }

    @PatchMapping("/{idOrden}/assign")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrdenServicioResponse>> asignarTecnicoYFecha(
            @PathVariable Integer idOrden,
            @Valid @RequestBody AsignarTecnicoRequest request) {

        exigirRol(RolNombre.ADMINISTRADOR, "Solo el Administrador está autorizado para asignar técnicos a las órdenes.");
        OrdenServicioResponse asignada = orderAssignmentService.asignarTecnicoYFecha(idOrden, request);

        return ResponseEntity.ok(ApiResponse.ok("Asignación guardada correctamente.", asignada));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<OrdenServicioResponse>>> listarOrdenes(
            @RequestParam(required = false) EstadoOperativoOrden estado,
            @RequestParam(required = false) Integer idCategoria,
            @RequestParam(required = false) Integer idTecnico,
            @RequestParam(required = false) String busqueda) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();

        List<OrdenServicioResponse> ordenes = orderService.listarOrdenesSegunRol(
                idUsuario, rol, estado, idCategoria, idTecnico, busqueda
        );
        return ResponseEntity.ok(ApiResponse.ok(ordenes));
    }

    @GetMapping("/{idOrden}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<OrdenServicioResponse>> consultarDetalleOrden(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        OrdenServicioResponse detalle = orderService.consultarDetalleOrden(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(detalle));
    }

    @GetMapping("/{idOrden}/history")
    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO_ESPECIALISTA', 'ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<List<HistorialEstadoOrdenResponse>>> consultarHistorialEstados(
            @PathVariable Integer idOrden) {

        Integer idUsuario = currentUserProvider.getCurrentUserId();
        String rol = currentUserProvider.getCurrentUserRole();
        List<HistorialEstadoOrdenResponse> historial = orderService.consultarHistorialEstados(idOrden, idUsuario, rol);

        return ResponseEntity.ok(ApiResponse.ok(historial));
    }

    private void exigirRol(RolNombre rolEsperado, String mensajeError) {
        String rolActual = currentUserProvider.getCurrentUserRole();
        if (!rolEsperado.name().equals(rolActual)) {
            throw new ForbiddenAccessException(mensajeError);
        }
    }
}