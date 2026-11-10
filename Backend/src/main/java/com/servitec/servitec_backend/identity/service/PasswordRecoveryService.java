package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.identity.dto.RestablecerContrasenaRequest;
import com.servitec.servitec_backend.identity.dto.RestablecerContrasenaResponse;
import com.servitec.servitec_backend.identity.dto.SolicitudRecuperacionRequest;
import com.servitec.servitec_backend.identity.dto.SolicitudRecuperacionResponse;

import com.servitec.servitec_backend.identity.entity.TokenCuenta;
import com.servitec.servitec_backend.identity.entity.Usuario;

import com.servitec.servitec_backend.identity.repository.SesionUsuarioRepository;
import com.servitec.servitec_backend.identity.repository.TokenCuentaRepository;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;

import com.servitec.servitec_backend.notifications.event.PasswordRecoveryEvent;

import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.TipoTokenCuenta;
import com.servitec.servitec_backend.shared.util.TokenHashUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class PasswordRecoveryService {

    private static final String MENSAJE_SOLICITUD =
            "Si el correo corresponde a una cuenta activa de Servitec, "
                    + "recibirás instrucciones para restablecer tu contraseña.";

    private final UsuarioRepository usuarioRepository;
    private final TokenCuentaRepository tokenCuentaRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final AccountTokenService accountTokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    private final long recoveryMinutes;

    public PasswordRecoveryService(
            UsuarioRepository usuarioRepository,
            TokenCuentaRepository tokenCuentaRepository,
            SesionUsuarioRepository sesionUsuarioRepository,
            AccountTokenService accountTokenService,
            PasswordEncoder passwordEncoder,
            ApplicationEventPublisher eventPublisher,
            Clock clock,
            @Value("${app.security.account-token.recovery-minutes:30}")
            long recoveryMinutes
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tokenCuentaRepository = tokenCuentaRepository;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.accountTokenService = accountTokenService;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.recoveryMinutes = recoveryMinutes;
    }

    /**
     * CV-11:
     * Solicita la recuperación de contraseña.
     *
     * La respuesta es idéntica tanto para correos registrados
     * como para correos que no existen.
     */
    @Transactional
    public SolicitudRecuperacionResponse solicitarRecuperacion(
            SolicitudRecuperacionRequest request
    ) {

        if (recoveryMinutes <= 0) {
            throw new IllegalStateException(
                    "La vigencia del token de recuperación debe ser positiva."
            );
        }

        String correo = request.correo()
                .trim()
                .toLowerCase(Locale.ROOT);

        Optional<Usuario> resultado =
                usuarioRepository.findWithRolByCorreo(correo);

        if (resultado.isPresent()) {

            Usuario usuario = resultado.get();

            // Solo las cuentas activas y con contraseña
            // pueden recuperar una contraseña existente.
            if (usuario.getEstadoCuenta() == EstadoCuenta.ACTIVA
                    && usuario.getContrasenaHash() != null
                    && !usuario.getContrasenaHash().isBlank()) {

                // Genera un token nuevo e invalida los anteriores
                // que todavía estén pendientes.
                String tokenPlano = accountTokenService.generarToken(
                        usuario,
                        TipoTokenCuenta.RECUPERACION_CONTRASENA,
                        Duration.ofMinutes(recoveryMinutes)
                );

                // El evento será procesado después del commit.
                eventPublisher.publishEvent(
                        new PasswordRecoveryEvent(
                                usuario.getCorreo(),
                                usuario.getNombreCompleto(),
                                tokenPlano
                        )
                );
            }
        }

        // Nunca revelar si el correo existe o no.
        return new SolicitudRecuperacionResponse(
                MENSAJE_SOLICITUD
        );
    }

    /**
     * CV-11:
     * Valida el enlace y establece una nueva contraseña.
     */
    @Transactional
    public RestablecerContrasenaResponse restablecerContrasena(
            RestablecerContrasenaRequest request
    ) {

        // 1. Confirmar que las contraseñas coincidan.
        if (!request.nuevaContrasena()
                .equals(request.confirmarContrasena())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La nueva contraseña y su confirmación no coinciden."
            );
        }

        // 2. Verificar el límite de BCrypt en bytes UTF-8.
        int longitudBytes = request.nuevaContrasena()
                .getBytes(StandardCharsets.UTF_8)
                .length;

        if (longitudBytes > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña supera la longitud máxima permitida."
            );
        }

        // 3. Calcular el hash del token recibido.
        String tokenHash = TokenHashUtil.hashSha256(
                request.token()
        );

        // 4. Buscar y bloquear el token para impedir
        // que dos solicitudes lo consuman simultáneamente.
        TokenCuenta token = tokenCuentaRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::tokenInvalido);

        LocalDateTime ahora = LocalDateTime.now(clock);

        // 5. Verificar tipo, uso previo y expiración.
        if (token.getTipoToken()
                != TipoTokenCuenta.RECUPERACION_CONTRASENA
                || token.getFechaUso() != null
                || !token.getFechaExpiracion().isAfter(ahora)) {

            throw tokenInvalido();
        }

        // 6. Obtener el usuario asociado.
        Usuario usuario = token.getUsuario();

        // 7. La recuperación solo aplica a cuentas activas.
        if (usuario.getEstadoCuenta() != EstadoCuenta.ACTIVA
                || usuario.getContrasenaHash() == null) {

            throw tokenInvalido();
        }

        // 8. Cifrar la nueva contraseña.
        String nuevaContrasenaHash = passwordEncoder.encode(
                request.nuevaContrasena()
        );

        usuario.setContrasenaHash(nuevaContrasenaHash);

        usuarioRepository.save(usuario);

        // 9. Invalidar todos los tokens de recuperación
        // que sigan pendientes para ese usuario.
        List<TokenCuenta> tokensPendientes =
                tokenCuentaRepository
                        .findAllByUsuario_IdUsuarioAndTipoTokenAndFechaUsoIsNull(
                                usuario.getIdUsuario(),
                                TipoTokenCuenta.RECUPERACION_CONTRASENA
                        );

        for (TokenCuenta tokenPendiente : tokensPendientes) {
            tokenPendiente.setFechaUso(ahora);
        }

        tokenCuentaRepository.saveAll(tokensPendientes);

        // 10. Revocar las sesiones anteriores del usuario.
        sesionUsuarioRepository.revocarSesionesActivasDeUsuario(
                usuario.getIdUsuario(),
                ahora
        );

        // 11. Confirmar el restablecimiento.
        return new RestablecerContrasenaResponse(
                "Tu contraseña fue restablecida correctamente. "
                        + "Ya puedes ingresar a Servitec con tus nuevas credenciales."
        );
    }

    /**
     * Mensaje genérico para cualquier token inválido,
     * expirado, utilizado o de tipo incorrecto.
     */
    private ResponseStatusException tokenInvalido() {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enlace de recuperación no es válido, ha expirado "
                        + "o ya fue utilizado. Inicia nuevamente el proceso."
        );
    }
}