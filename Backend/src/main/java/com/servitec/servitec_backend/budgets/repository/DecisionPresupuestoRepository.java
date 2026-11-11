package com.servitec.servitec_backend.budgets.repository;

import com.servitec.servitec_backend.budgets.entity.DecisionPresupuesto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DecisionPresupuestoRepository extends JpaRepository<DecisionPresupuesto, Integer> {

    boolean existsByPresupuesto_IdPresupuesto(Integer idPresupuesto);

    @Query("""
            SELECT d FROM DecisionPresupuesto d
            JOIN FETCH d.clienteDecisor
            WHERE d.presupuesto.idPresupuesto = :idPresupuesto
            """)
    Optional<DecisionPresupuesto> findByIdPresupuestoWithCliente(@Param("idPresupuesto") Integer idPresupuesto);

    @Query("""
            SELECT d FROM DecisionPresupuesto d
            JOIN FETCH d.clienteDecisor
            JOIN FETCH d.presupuesto p
            WHERE p.ordenServicio.idOrden = :idOrden
            ORDER BY d.fechaHoraDecision ASC
            """)
    List<DecisionPresupuesto> findAllByIdOrden(@Param("idOrden") Integer idOrden);
}
