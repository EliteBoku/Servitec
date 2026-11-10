package com.servitec.servitec_backend.inventory.repository;

import com.servitec.servitec_backend.inventory.entity.MovimientoInventario;
import com.servitec.servitec_backend.shared.enums.OrigenMovimientoInventario;
import com.servitec.servitec_backend.shared.enums.TipoMovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Integer> {

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.repuesto r
            JOIN FETCH m.usuarioRegistro u
            WHERE (:idRepuesto IS NULL OR r.idRepuesto = :idRepuesto)
              AND (:tipoMovimiento IS NULL OR m.tipoMovimiento = :tipoMovimiento)
              AND (:origenMovimiento IS NULL OR m.origenMovimiento = :origenMovimiento)
              AND (:fechaDesde IS NULL OR m.fechaHoraMovimiento >= :fechaDesde)
              AND (:fechaHasta IS NULL OR m.fechaHoraMovimiento <= :fechaHasta)
            ORDER BY m.fechaHoraMovimiento DESC, m.idMovimiento DESC
            """)
    List<MovimientoInventario> buscarHistorialMovimientos(
            @Param("idRepuesto") Integer idRepuesto,
            @Param("tipoMovimiento") TipoMovimientoInventario tipoMovimiento,
            @Param("origenMovimiento") OrigenMovimientoInventario origenMovimiento,
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta
    );
}