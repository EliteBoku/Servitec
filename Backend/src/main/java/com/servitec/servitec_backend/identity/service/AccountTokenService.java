package com.servitec.servitec_backend.identity.service;

import com.servitec.servitec_backend.identity.entity.TokenCuenta;
import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.TokenCuentaRepository;
import com.servitec.servitec_backend.shared.enums.TipoTokenCuenta;
import com.servitec.servitec_backend.shared.util.TokenHashUtil;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AccountTokenService {

    private static final ZoneId ZONA_HORARIA =
            ZoneId.of("America/Bogota");

    private final TokenCuentaRepository tokenCuentaRepository;

    public AccountTokenService(
            TokenCuentaRepository tokenCuentaRepository
    ) {
        this.tokenCuentaRepository = tokenCuentaRepository;
    }

    /**
     * Genera un token seguro, invalida los anteriores
     * del mismo tipo y almacena únicamente su hash.
     *
     * Este método debe ejecutarse dentro de una transacción.
     */
    @Transactional
    public String generarToken(
            Usuario usuario,
            TipoTokenCuenta tipoToken,
            Duration vigencia
    ) {

        if (usuario == null || usuario.getIdUsuario() == null) {
            throw new IllegalArgumentException(
                    "El usuario debe estar guardado antes de generar el token."
            );
        }

        if (tipoToken == null) {
            throw new IllegalArgumentException(
                    "El tipo de token es obligatorio."
            );
        }

        if (vigencia == null || vigencia.isZero() || vigencia.isNegative()) {
            throw new IllegalArgumentException(
                    "La vigencia del token debe ser positiva."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_HORARIA);

        // Invalidar tokens anteriores que no hayan sido utilizados.
        List<TokenCuenta> tokensAnteriores =
                tokenCuentaRepository
                        .findAllByUsuario_IdUsuarioAndTipoTokenAndFechaUsoIsNull(
                                usuario.getIdUsuario(),
                                tipoToken
                        );

        tokensAnteriores.forEach(
                token -> token.setFechaUso(ahora)
        );

        if (!tokensAnteriores.isEmpty()) {
            tokenCuentaRepository.saveAll(tokensAnteriores);
        }

        // Token original: solamente se entrega al proceso de notificación.
        String tokenPlano = TokenHashUtil.generarTokenSeguro();

        // En la base de datos se almacena exclusivamente el SHA-256.
        String tokenHash = TokenHashUtil.hashSha256(tokenPlano);

        TokenCuenta nuevoToken = new TokenCuenta(
                usuario,
                tipoToken,
                tokenHash,
                ahora,
                ahora.plus(vigencia),
                null
        );

        tokenCuentaRepository.save(nuevoToken);

        return tokenPlano;
    }
}