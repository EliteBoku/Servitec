package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.identity.dto.CrearUsuarioRequest;
import com.servitec.servitec_backend.identity.dto.UsuarioCreadoResponse;

import com.servitec.servitec_backend.identity.entity.Rol;
import com.servitec.servitec_backend.identity.entity.Usuario;

import com.servitec.servitec_backend.identity.repository.RolRepository;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;

import com.servitec.servitec_backend.notifications.event.AccountActivationEvent;

import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import com.servitec.servitec_backend.shared.enums.TipoTokenCuenta;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class UserAdminService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Bogota");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AccountTokenService accountTokenService;
    private final ApplicationEventPublisher eventPublisher;

    private final long activationHours;

    public UserAdminService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            AccountTokenService accountTokenService,
            ApplicationEventPublisher eventPublisher,
            @Value("${app.security.account-token.activation-hours:24}")
            long activationHours
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.accountTokenService = accountTokenService;
        this.eventPublisher = eventPublisher;
        this.activationHours = activationHours;
    }

    /**
     * Crea una cuenta de Cliente o Técnico especialista
     * y solicita el envío del enlace de activación.
     */
    @Transactional
    public UsuarioCreadoResponse crearUsuario(
            CrearUsuarioRequest request
    ) {

        validarRolPermitido(request.rol());

        if (activationHours <= 0) {
            throw new IllegalStateException(
                    "La vigencia de activación debe ser mayor que cero."
            );
        }

        String nombre = request.nombreCompleto().trim();

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Comprobar correo duplicado.
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una cuenta registrada con ese correo electrónico."
            );
        }

        // Obtener el rol existente.
        Rol rol = rolRepository.findByNombre(request.rol())
                .orElseThrow(() -> new IllegalStateException(
                        "El rol solicitado no existe en la base de datos."
                ));

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        // Crear cuenta pendiente, sin contraseña.
        Usuario nuevoUsuario = new Usuario(
                rol,
                nombre,
                correo,
                null,
                EstadoCuenta.PENDIENTE_ACTIVACION,
                ahora,
                null
        );

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        // Generar token seguro con vigencia configurable.
        String tokenPlano = accountTokenService.generarToken(
                usuarioGuardado,
                TipoTokenCuenta.ACTIVACION_CUENTA,
                Duration.ofHours(activationHours)
        );

        // Publicar evento dentro de la transacción.
        // El listener lo procesa después del commit.
        eventPublisher.publishEvent(
                new AccountActivationEvent(
                        usuarioGuardado.getCorreo(),
                        usuarioGuardado.getNombreCompleto(),
                        tokenPlano
                )
        );

        return new UsuarioCreadoResponse(
                usuarioGuardado.getIdUsuario(),
                usuarioGuardado.getNombreCompleto(),
                usuarioGuardado.getCorreo(),
                request.rol(),
                usuarioGuardado.getEstadoCuenta(),
                "La cuenta fue creada y se solicitó el envío del correo de activación."
        );
    }

    /**
     * CV-10 permite crear únicamente Clientes y Técnicos.
     */
    private void validarRolPermitido(RolNombre rol) {

        if (rol != RolNombre.CLIENTE
                && rol != RolNombre.TECNICO_ESPECIALISTA) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se pueden crear cuentas de Cliente o Técnico especialista."
            );
        }
    }
}