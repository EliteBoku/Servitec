package com.servitec.servitec_backend.budgets.repository;

import com.servitec.servitec_backend.budgets.entity.Presupuesto;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import com.servitec.servitec_backend.shared.enums.TipoPresupuesto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PresupuestoRepository extends JpaRepository<Presupuesto, Integer> {

    boolean existsByOrdenServicio_IdOrdenAndTipoPresupuesto(Integer idOrden, TipoPresupuesto tipoPresupuesto);

    boolean existsByOrdenServicio_IdOrdenAndTipoPresupuestoAndEstado(
            Integer idOrden,
            TipoPresupuesto tipoPresupuesto,
            EstadoPresupuesto estado
    );

    boolean existsByAutorizacionNuevaCotizacion_IdAutorizacion(Integer idAutorizacion);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM Presupuesto p
            JOIN FETCH p.ordenServicio o
            JOIN FETCH o.cliente
            JOIN FETCH p.tecnicoCreador
            LEFT JOIN FETCH p.autorizacionNuevaCotizacion
            WHERE p.idPresupuesto = :idPresupuesto
            """)
    Optional<Presupuesto> findByIdWithLock(@Param("idPresupuesto") Integer idPresupuesto);

    @Query("""
            SELECT p FROM Presupuesto p
            JOIN FETCH p.ordenServicio o
            JOIN FETCH o.cliente
            JOIN FETCH p.tecnicoCreador
            LEFT JOIN FETCH p.autorizacionNuevaCotizacion
            WHERE p.idPresupuesto = :idPresupuesto
            """)
    Optional<Presupuesto> findByIdWithDetails(@Param("idPresupuesto") Integer idPresupuesto);

    @Query("""
            SELECT p FROM Presupuesto p
            JOIN FETCH p.ordenServicio o
            JOIN FETCH p.tecnicoCreador
            WHERE o.idOrden = :idOrden
              AND p.tipoPresupuesto = :tipoPresupuesto
            """)
    Optional<Presupuesto> findByIdOrdenAndTipoPresupuesto(
            @Param("idOrden") Integer idOrden,
            @Param("tipoPresupuesto") TipoPresupuesto tipoPresupuesto
    );

    @Query("""
            SELECT p FROM Presupuesto p
            JOIN FETCH p.ordenServicio o
            JOIN FETCH p.tecnicoCreador
            LEFT JOIN FETCH p.autorizacionNuevaCotizacion
            WHERE o.idOrden = :idOrden
            ORDER BY p.fechaEmision ASC, p.idPresupuesto ASC
            """)
    List<Presupuesto> findAllByIdOrdenOrderByFechaEmisionAsc(@Param("idOrden") Integer idOrden);
}