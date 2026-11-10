package com.servitec.servitec_backend.inventory.controller;

import com.servitec.servitec_backend.inventory.dto.ActualizarRepuestoRequest;
import com.servitec.servitec_backend.inventory.dto.CrearRepuestoRequest;
import com.servitec.servitec_backend.inventory.dto.RepuestoResponse;
import com.servitec.servitec_backend.inventory.service.CatalogService;
import com.servitec.servitec_backend.security.CurrentUserProvider;
import com.servitec.servitec_backend.shared.api.ApiResponse;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;
import com.servitec.servitec_backend.shared.exception.ForbiddenAccessException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/catalog")
public class CatalogController {

    private final CatalogService catalogService;
    private final CurrentUserProvider currentUserProvider;

    public CatalogController(CatalogService catalogService, CurrentUserProvider currentUserProvider) {
        this.catalogService = catalogService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<RepuestoResponse>> crearRepuesto(
            @Valid @RequestBody CrearRepuestoRequest request) {

        exigirRolAdministrador();
        Integer idAdmin = currentUserProvider.getCurrentUserId();
        RepuestoResponse creado = catalogService.crearRepuesto(request, idAdmin);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Elemento registrado correctamente en el catálogo.", creado));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<List<RepuestoResponse>>> listarCatalogo(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) TipoItemCatalogo tipoItem,
            @RequestParam(required = false) Boolean activo) {

        String rolActual = currentUserProvider.getCurrentUserRole();
        exigirRolAdminOTecnico(rolActual);

        // REQ-10 y REQ-27: El Técnico especialista solo puede consultar elementos activos
        Boolean filtroActivo = RolNombre.TECNICO_ESPECIALISTA.name().equals(rolActual)
                ? Boolean.TRUE
                : activo;

        List<RepuestoResponse> elementos = catalogService.listarCatalogo(busqueda, tipoItem, filtroActivo);
        return ResponseEntity.ok(ApiResponse.ok(elementos));
    }

    @GetMapping("/{idRepuesto}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO_ESPECIALISTA')")
    public ResponseEntity<ApiResponse<RepuestoResponse>> consultarPorId(
            @PathVariable Integer idRepuesto) {

        String rolActual = currentUserProvider.getCurrentUserRole();
        exigirRolAdminOTecnico(rolActual);

        boolean soloActivos = RolNombre.TECNICO_ESPECIALISTA.name().equals(rolActual);
        RepuestoResponse elemento = catalogService.consultarPorId(idRepuesto, soloActivos);
        return ResponseEntity.ok(ApiResponse.ok(elemento));
    }

    @PutMapping("/{idRepuesto}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<RepuestoResponse>> actualizarRepuesto(
            @PathVariable Integer idRepuesto,
            @Valid @RequestBody ActualizarRepuestoRequest request) {

        exigirRolAdministrador();
        RepuestoResponse actualizado = catalogService.actualizarRepuesto(idRepuesto, request);
        return ResponseEntity.ok(ApiResponse.ok("Elemento del catálogo actualizado correctamente.", actualizado));
    }

    @PatchMapping("/{idRepuesto}/deactivate")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<RepuestoResponse>> desactivarRepuesto(
            @PathVariable Integer idRepuesto) {

        exigirRolAdministrador();
        RepuestoResponse desactivado = catalogService.cambiarEstadoActivo(idRepuesto, false);
        return ResponseEntity.ok(ApiResponse.ok("Elemento desactivado correctamente.", desactivado));
    }

    @PatchMapping("/{idRepuesto}/activate")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<ApiResponse<RepuestoResponse>> reactivarRepuesto(
            @PathVariable Integer idRepuesto) {

        exigirRolAdministrador();
        RepuestoResponse reactivado = catalogService.cambiarEstadoActivo(idRepuesto, true);
        return ResponseEntity.ok(ApiResponse.ok("Elemento reactivado correctamente.", reactivado));
    }

    private void exigirRolAdministrador() {
        String rol = currentUserProvider.getCurrentUserRole();
        if (!RolNombre.ADMINISTRADOR.name().equals(rol)) {
            throw new ForbiddenAccessException("Solo el Administrador está autorizado para gestionar el catálogo.");
        }
    }

    private void exigirRolAdminOTecnico(String rol) {
        if (!RolNombre.ADMINISTRADOR.name().equals(rol)
                && !RolNombre.TECNICO_ESPECIALISTA.name().equals(rol)) {
            throw new ForbiddenAccessException("No tienes permisos para consultar el catálogo de repuestos e insumos.");
        }
    }
}