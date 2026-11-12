package com.servitec.servitec_backend.identity.controller;

import com.servitec.servitec_backend.identity.dto.CrearUsuarioRequest;
import com.servitec.servitec_backend.identity.dto.UsuarioCreadoResponse;
import com.servitec.servitec_backend.identity.service.UserAdminService;
import com.servitec.servitec_backend.shared.enums.RolNombre;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    /**
     * CV-10:
     * Solo el Administrador puede crear cuentas de Cliente y Técnico.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UsuarioCreadoResponse> crearUsuario(
            @Valid @RequestBody CrearUsuarioRequest request
    ) {

        UsuarioCreadoResponse response =
                userAdminService.crearUsuario(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * CV-10 y CV-03:
     * Lista las cuentas registradas en el sistema (con filtro opcional por rol).
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<List<UsuarioCreadoResponse>> listarUsuarios(
            @RequestParam(required = false) RolNombre rol
    ) {

        List<UsuarioCreadoResponse> usuarios =
                userAdminService.listarUsuarios(rol);

        return ResponseEntity.ok(usuarios);
    }
}