package com.servitec.servitec_backend.inventory.repository;

import com.servitec.servitec_backend.inventory.entity.ReservaInventario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReservaInventarioRepository extends JpaRepository<ReservaInventario, Integer> {

    Optional<ReservaInventario> findByIdDetallePresupuesto(Integer idDetallePresupuesto);

    boolean existsByIdDetallePresupuesto(Integer idDetallePresupuesto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReservaInventario r WHERE r.idDetallePresupuesto IN :idsDetalle ORDER BY r.idReserva ASC")
    List<ReservaInventario> findAllByIdDetallePresupuestoInWithLock(@Param("idsDetalle") Collection<Integer> idsDetalle);

    @Query(value = """
            SELECT COALESCE(SUM(ri.cantidad_reservada), 0)
            FROM reserva_inventario ri
            INNER JOIN detalle_presupuesto dp ON ri.id_detalle_presupuesto = dp.id_detalle
            WHERE dp.id_repuesto = :idRepuesto
              AND ri.estado_reserva = 'ACTIVA'
            """, nativeQuery = true)
    BigDecimal sumCantidadReservadaActivaByRepuesto(@Param("idRepuesto") Integer idRepuesto);
}