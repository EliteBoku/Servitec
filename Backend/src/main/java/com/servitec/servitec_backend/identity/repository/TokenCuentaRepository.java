package com.servitec.servitec_backend.identity.repository;

import com.servitec.servitec_backend.identity.entity.TokenCuenta;
import com.servitec.servitec_backend.shared.enums.TipoTokenCuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenCuentaRepository
        extends JpaRepository<TokenCuenta, Integer> {

    Optional<TokenCuenta> findByTokenHash(String tokenHash);

    Optional<TokenCuenta>
    findByTokenHashAndFechaUsoIsNullAndFechaExpiracionAfter(
            String tokenHash,
            LocalDateTime fechaActual
    );

    List<TokenCuenta>
    findAllByUsuario_IdUsuarioAndTipoTokenAndFechaUsoIsNull(
            Integer idUsuario,
            TipoTokenCuenta tipoToken
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT token
        FROM TokenCuenta token
        WHERE token.tokenHash = :tokenHash
        """)
    Optional<TokenCuenta> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}