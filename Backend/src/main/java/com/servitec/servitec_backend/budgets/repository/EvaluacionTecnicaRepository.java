package com.servitec.servitec_backend.budgets.repository;

import com.servitec.servitec_backend.budgets.entity.EvaluacionTecnica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluacionTecnicaRepository extends JpaRepository<EvaluacionTecnica, Integer> {

    boolean existsByPresupuestoRechazado_IdPresupuesto(Integer idPresupuestoRechazado);

    @Query("""
            SELECT e FROM EvaluacionTecnica e
            JOIN FETCH e.tecnico
            JOIN FETCH e.presupuestoRechazado p
            WHERE p.idPresupuesto = :idPresupuestoRechazado
            """)
    Optional<EvaluacionTecnica> findByIdPresupuestoRechazadoWithDetails(
            @Param("idPresupuestoRechazado") Integer idPresupuestoRechazado
    );

    @Query("""
            SELECT e FROM EvaluacionTecnica e
            JOIN FETCH e.tecnico
            JOIN FETCH e.presupuestoRechazado p
            WHERE p.ordenServicio.idOrden = :idOrden
            ORDER BY e.fechaHora DESC, e.idEvaluacion DESC
            """)
    List<EvaluacionTecnica> findAllByIdOrdenOrderByFechaHoraDesc(@Param("idOrden") Integer idOrden);
}