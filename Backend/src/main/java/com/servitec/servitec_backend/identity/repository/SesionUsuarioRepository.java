package com.servitec.servitec_backend.identity.repository;

import com.servitec.servitec_backend.identity.entity.SesionUsuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SesionUsuarioRepository
        extends JpaRepository<SesionUsuario, Integer> {

    Optional<SesionUsuario> findByRefreshTokenHash(
            String refreshTokenHash
    );

    Optional<SesionUsuario>
    findByRefreshTokenHashAndFechaHoraRevocacionIsNullAndFechaHoraExpiracionAfter(
            String refreshTokenHash,
            LocalDateTime fechaActual
    );

    List<SesionUsuario>
    findAllByUsuario_IdUsuarioAndFechaHoraRevocacionIsNull(
            Integer idUsuario
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sesion
        FROM SesionUsuario sesion
        WHERE sesion.refreshTokenHash = :refreshTokenHash
        """)
    Optional<SesionUsuario> findByRefreshTokenHashForUpdate(
            @Param("refreshTokenHash") String refreshTokenHash
    );
}