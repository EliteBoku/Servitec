package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.identity.entity.SesionUsuario;
import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.SesionUsuarioRepository;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.security.jwt.JwtProperties;
import com.servitec.servitec_backend.security.jwt.JwtTokenService;
import com.servitec.servitec_backend.shared.util.TokenHashUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionManagementService {

    private final SesionUsuarioRepository sesionRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final UsuarioRepository usuarioRepository;

    public SessionManagementService(
            SesionUsuarioRepository sesionRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            Clock clock, UsuarioRepository usuarioRepository
    ) {
        this.sesionRepository = sesionRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public SessionTokens createSession(Integer idUsuario) {

        LocalDateTime ahora = LocalDateTime.now(clock);
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el usuario autenticado."
                ));

        String refreshToken = TokenHashUtil.generateSecureToken();

        LocalDateTime expiracion = ahora.plusDays(
                jwtProperties.getRefreshTokenExpirationDays()
        );

        SesionUsuario sesion = new SesionUsuario(
                usuario,
                TokenHashUtil.sha256Hex(refreshToken),
                ahora,
                ahora,
                expiracion,
                null
        );

        SesionUsuario sesionGuardada =
                sesionRepository.save(sesion);

        return buildTokens(usuario, sesionGuardada, refreshToken);
    }

    @Transactional
    public Optional<SessionTokens> refresh(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return Optional.empty();
        }

        String refreshHash = TokenHashUtil.sha256Hex(refreshToken);

        Optional<SesionUsuario> resultado =
                sesionRepository.findByRefreshTokenHashForUpdate(refreshHash);

        if (resultado.isEmpty()) {
            return Optional.empty();
        }

        SesionUsuario sesion = resultado.get();

        LocalDateTime ahora = LocalDateTime.now(clock);

        LocalDateTime limiteInactividad = ahora.minusMinutes(
                jwtProperties.getInactivityTimeoutMinutes()
        );

        boolean revocada = sesion.getFechaHoraRevocacion() != null;

        boolean expirada =
                !sesion.getFechaHoraExpiracion().isAfter(ahora);

        boolean inactiva =
                sesion.getFechaHoraUltimaActividad()
                        .isBefore(limiteInactividad);

        if (revocada || expirada || inactiva) {

            if (!revocada) {
                sesion.setFechaHoraRevocacion(ahora);
                sesionRepository.save(sesion);
            }

            return Optional.empty();
        }

        String nuevoRefreshToken =
                TokenHashUtil.generateSecureToken();

        sesion.setRefreshTokenHash(
                TokenHashUtil.sha256Hex(nuevoRefreshToken)
        );

        sesion.setFechaHoraUltimaActividad(ahora);

        sesionRepository.save(sesion);

        return Optional.of(
                buildTokens(
                        sesion.getUsuario(),
                        sesion,
                        nuevoRefreshToken
                )
        );
    }

    @Transactional
    public void revoke(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String refreshHash = TokenHashUtil.sha256Hex(refreshToken);

        sesionRepository.findByRefreshTokenHashForUpdate(refreshHash)
                .ifPresent(sesion -> {

                    if (sesion.getFechaHoraRevocacion() == null) {

                        sesion.setFechaHoraRevocacion(
                                LocalDateTime.now(clock)
                        );

                        sesionRepository.save(sesion);
                    }
                });
    }

    @Transactional
    public void revokeAllForUser(Integer idUsuario) {

        List<SesionUsuario> sesiones =
                sesionRepository
                        .findAllByUsuario_IdUsuarioAndFechaHoraRevocacionIsNull(
                                idUsuario
                        );

        LocalDateTime ahora = LocalDateTime.now(clock);

        sesiones.forEach(sesion ->
                sesion.setFechaHoraRevocacion(ahora)
        );

        sesionRepository.saveAll(sesiones);
    }

    private SessionTokens buildTokens(
            Usuario usuario,
            SesionUsuario sesion,
            String refreshToken
    ) {

        long refreshExpiresIn = Math.max(
                0,
                Duration.between(
                        LocalDateTime.now(clock),
                        sesion.getFechaHoraExpiracion()
                ).toSeconds()
        );

        String accessToken =
                jwtTokenService.generateAccessToken(usuario, sesion);

        return new SessionTokens(
                accessToken,
                refreshToken,
                jwtTokenService.getAccessTokenExpirationSeconds(),
                refreshExpiresIn,
                usuario.getIdUsuario(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getRol().getNombre().name()
        );
    }

    public record SessionTokens(
            String accessToken,
            String refreshToken,
            long accessTokenExpiresInSeconds,
            long refreshTokenExpiresInSeconds,
            Integer idUsuario,
            String nombreCompleto,
            String correo,
            String rol
    ) {
    }
}