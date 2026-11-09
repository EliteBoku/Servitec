package com.servitec.servitec_backend.security.jwt;
import com.servitec.servitec_backend.identity.entity.SesionUsuario;
import com.servitec.servitec_backend.identity.repository.SesionUsuarioRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class SessionActivityJwtValidator
        implements OAuth2TokenValidator<Jwt> {

    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public SessionActivityJwtValidator(
            SesionUsuarioRepository sesionUsuarioRepository,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        Integer idSesion = obtenerIdSesion(jwt);

        if (idSesion == null) {
            return invalidToken(
                    "El token no contiene una identificación de sesión válida."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(clock);

        LocalDateTime limiteInactividad = ahora.minusMinutes(
                jwtProperties.getInactivityTimeoutMinutes()
        );

        try {

            Optional<SesionUsuario> resultado =
                    sesionUsuarioRepository.findById(idSesion);

            if (resultado.isEmpty()) {
                return invalidToken(
                        "La sesión asociada al token no existe."
                );
            }

            SesionUsuario sesion = resultado.get();

            if (sesion.getFechaHoraRevocacion() != null) {
                return invalidToken(
                        "La sesión ha sido revocada."
                );
            }

            if (!sesion.getFechaHoraExpiracion().isAfter(ahora)) {
                return invalidToken(
                        "La sesión ha expirado."
                );
            }

            if (sesion.getFechaHoraUltimaActividad()
                    .isBefore(limiteInactividad)) {

                return invalidToken(
                        "La sesión ha expirado por inactividad."
                );
            }

            int actualizadas =
                    sesionUsuarioRepository.actualizarActividadSiVigente(
                            idSesion,
                            ahora,
                            limiteInactividad
                    );

            if (actualizadas == 0) {
                return invalidToken(
                        "La sesión ya no está vigente."
                );
            }

            return OAuth2TokenValidatorResult.success();

        } catch (DataAccessException exception) {

            return invalidToken(
                    "No fue posible verificar la sesión."
            );
        }
    }

    private Integer obtenerIdSesion(Jwt jwt) {

        Object claim = jwt.getClaims().get("sid");

        if (claim == null) {
            return null;
        }

        try {

            if (claim instanceof Number number) {

                int id = number.intValue();

                return id > 0 ? id : null;
            }

            int id = Integer.parseInt(claim.toString());

            return id > 0 ? id : null;

        } catch (NumberFormatException exception) {

            return null;
        }
    }

    private OAuth2TokenValidatorResult invalidToken(String mensaje) {

        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                mensaje,
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}