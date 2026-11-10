package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.identity.dto.ActivarCuentaRequest;
import com.servitec.servitec_backend.identity.dto.CuentaActivadaResponse;

import com.servitec.servitec_backend.identity.entity.TokenCuenta;
import com.servitec.servitec_backend.identity.entity.Usuario;

import com.servitec.servitec_backend.identity.repository.TokenCuentaRepository;

import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.TipoTokenCuenta;
import com.servitec.servitec_backend.shared.util.TokenHashUtil;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountActivationService {

    private final TokenCuentaRepository tokenCuentaRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public AccountActivationService(
            TokenCuentaRepository tokenCuentaRepository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.tokenCuentaRepository = tokenCuentaRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /**
     * CV-10:
     * Valida el enlace de activación y permite establecer
     * la contraseña inicial del usuario.
     */
    @Transactional
    public CuentaActivadaResponse activarCuenta(
            ActivarCuentaRequest request
    ) {

        // 1. Validar coincidencia de contraseñas.
        if (!request.contrasena().equals(request.confirmarContrasena())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña y su confirmación no coinciden."
            );
        }

        // 2. Validar límite real de BCrypt en bytes UTF-8.
        int longitudBytes = request.contrasena()
                .getBytes(StandardCharsets.UTF_8)
                .length;

        if (longitudBytes > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contraseña supera la longitud máxima permitida."
            );
        }

        // 3. Calcular SHA-256 del token recibido.
        String tokenHash = TokenHashUtil.hashSha256(
                request.token()
        );

        // 4. Buscar el token con bloqueo de escritura.
        TokenCuenta token = tokenCuentaRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(this::tokenInvalido);

        LocalDateTime ahora = LocalDateTime.now(clock);

        // 5. Verificar tipo, uso y vigencia.
        if (token.getTipoToken() != TipoTokenCuenta.ACTIVACION_CUENTA
                || token.getFechaUso() != null
                || !token.getFechaExpiracion().isAfter(ahora)) {

            throw tokenInvalido();
        }

        // 6. Obtener el usuario asociado.
        Usuario usuario = token.getUsuario();

        // 7. Comprobar que la cuenta realmente esté pendiente.
        if (usuario.getEstadoCuenta() != EstadoCuenta.PENDIENTE_ACTIVACION
                || usuario.getContrasenaHash() != null) {

            throw tokenInvalido();
        }

        // 8. Cifrar la contraseña mediante BCrypt.
        String contrasenaHash = passwordEncoder.encode(
                request.contrasena()
        );

        // 9. Activar la cuenta.
        usuario.setContrasenaHash(contrasenaHash);
        usuario.setEstadoCuenta(EstadoCuenta.ACTIVA);
        usuario.setFechaActivacion(ahora);

        // 10. Invalidar todos los tokens de activación
        // que todavía estén pendientes para este usuario.
        List<TokenCuenta> tokensPendientes =
                tokenCuentaRepository
                        .findAllByUsuario_IdUsuarioAndTipoTokenAndFechaUsoIsNull(
                                usuario.getIdUsuario(),
                                TipoTokenCuenta.ACTIVACION_CUENTA
                        );

        for (TokenCuenta tokenPendiente : tokensPendientes) {
            tokenPendiente.setFechaUso(ahora);
        }

        tokenCuentaRepository.saveAll(tokensPendientes);

        // 11. Confirmar resultado de negocio.
        return new CuentaActivadaResponse(
                "Tu cuenta Servitec ha sido activada correctamente. "
                        + "Ya puedes iniciar sesión."
        );
    }

    /**
     * Respuesta genérica para evitar revelar detalles
     * sobre el estado interno de los tokens.
     */
    private ResponseStatusException tokenInvalido() {

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "El enlace de activación no es válido, ha expirado "
                        + "o ya fue utilizado. Solicita un nuevo enlace."
        );
    }
}