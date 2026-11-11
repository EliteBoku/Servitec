package com.servitec.servitec_backend.budgets.repository;

import com.servitec.servitec_backend.budgets.entity.AutorizacionNuevaCotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AutorizacionNuevaCotizacionRepository extends JpaRepository<AutorizacionNuevaCotizacion, Integer> {

    boolean existsByPresupuestoRechazado_IdPresupuesto(Integer idPresupuestoRechazado);

    @Query("""
            SELECT a FROM AutorizacionNuevaCotizacion a
            JOIN FETCH a.presupuestoRechazado p
            JOIN FETCH p.ordenServicio o
            JOIN FETCH a.evaluacionTecnica e
            JOIN FETCH a.administrador
            WHERE a.idAutorizacion = :idAutorizacion
            """)
    Optional<AutorizacionNuevaCotizacion> findByIdWithDetails(@Param("idAutorizacion") Integer idAutorizacion);

    @Query("""
            SELECT a FROM AutorizacionNuevaCotizacion a
            JOIN FETCH a.presupuestoRechazado p
            JOIN FETCH a.evaluacionTecnica e
            JOIN FETCH a.administrador
            WHERE p.idPresupuesto = :idPresupuestoRechazado
            """)
    Optional<AutorizacionNuevaCotizacion> findByIdPresupuestoRechazado(
            @Param("idPresupuestoRechazado") Integer idPresupuestoRechazado
    );

    @Query("""
            SELECT a FROM AutorizacionNuevaCotizacion a
            JOIN FETCH a.presupuestoRechazado p
            JOIN FETCH a.evaluacionTecnica e
            JOIN FETCH a.administrador
            WHERE p.ordenServicio.idOrden = :idOrden
            ORDER BY a.fechaHoraAutorizacion DESC
            """)
    List<AutorizacionNuevaCotizacion> findAllByIdOrden(@Param("idOrden") Integer idOrden);
}