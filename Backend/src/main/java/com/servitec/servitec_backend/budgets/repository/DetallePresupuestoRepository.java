package com.servitec.servitec_backend.budgets.repository;

import com.servitec.servitec_backend.budgets.entity.DetallePresupuesto;
import com.servitec.servitec_backend.shared.enums.EstadoPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DetallePresupuestoRepository extends JpaRepository<DetallePresupuesto, Integer> {

    @Query("""
            SELECT d FROM DetallePresupuesto d
            JOIN FETCH d.repuesto
            WHERE d.presupuesto.idPresupuesto = :idPresupuesto
            ORDER BY d.idDetalle ASC
            """)
    List<DetallePresupuesto> findByIdPresupuestoWithRepuesto(@Param("idPresupuesto") Integer idPresupuesto);

    @Query("""
            SELECT d FROM DetallePresupuesto d
            JOIN FETCH d.repuesto
            JOIN FETCH d.presupuesto p
            WHERE p.ordenServicio.idOrden = :idOrden
              AND p.estado = :estadoPresupuesto
            ORDER BY p.idPresupuesto ASC, d.idDetalle ASC
            """)
    List<DetallePresupuesto> findAllByIdOrdenAndEstadoPresupuesto(
            @Param("idOrden") Integer idOrden,
            @Param("estadoPresupuesto") EstadoPresupuesto estadoPresupuesto
    );
}