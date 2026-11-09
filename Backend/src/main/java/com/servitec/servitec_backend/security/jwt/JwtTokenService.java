package com.servitec.servitec_backend.security.jwt;

import com.servitec.servitec_backend.identity.entity.SesionUsuario;
import com.servitec.servitec_backend.identity.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            JwtProperties jwtProperties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
    }

    public String generateAccessToken(
            Usuario usuario,
            SesionUsuario sesion
    ) {

        Instant issuedAt = clock.instant();

        Instant expiresAt = issuedAt.plus(
                Duration.ofMinutes(
                        jwtProperties.getAccessTokenExpirationMinutes()
                )
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(usuario.getIdUsuario().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("sid", sesion.getIdSesion())
                .claim("role", usuario.getRol().getNombre().name())
                .claim("email", usuario.getCorreo())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();
    }

    public long getAccessTokenExpirationSeconds() {
        return Duration.ofMinutes(
                jwtProperties.getAccessTokenExpirationMinutes()
        ).toSeconds();
    }
}