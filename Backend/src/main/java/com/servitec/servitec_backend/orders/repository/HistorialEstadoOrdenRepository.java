package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.HistorialEstadoOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HistorialEstadoOrdenRepository extends JpaRepository<HistorialEstadoOrden, Integer> {

    @Query("""
            SELECT h FROM HistorialEstadoOrden h
            JOIN FETCH h.usuarioResponsable
            WHERE h.ordenServicio.idOrden = :idOrden
            ORDER BY h.fechaHoraCambio ASC, h.idHistorialEstado ASC
            """)
    List<HistorialEstadoOrden> findHistorialPorOrden(@Param("idOrden") Integer idOrden);
}