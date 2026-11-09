package com.servitec.servitec_backend.security.jwt;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Base64;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class NimbusJwtConfig {

    @Bean
    public SecretKey jwtSecretKey(JwtProperties properties) {

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(properties.getSecret());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "La clave JWT debe estar codificada en Base64.",
                    exception
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "La clave JWT debe contener al menos 32 bytes."
            );
        }

        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtSecretKey)
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties properties,
            SessionActivityJwtValidator sessionActivityJwtValidator
    ) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        OAuth2TokenValidator<Jwt> standardValidator =
                JwtValidators.createDefaultWithIssuer(
                        properties.getIssuer()
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        standardValidator,
                        sessionActivityJwtValidator
                )
        );

        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock applicationClock() {

        return Clock.system(
                ZoneId.of("America/Bogota")
        );
    }
}
