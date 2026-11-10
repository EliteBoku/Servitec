package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.LiquidacionEconomica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LiquidacionEconomicaRepository extends JpaRepository<LiquidacionEconomica, Integer> {

    @Query("""
            SELECT l FROM LiquidacionEconomica l
            JOIN FETCH l.cancelacionOrden c
            JOIN FETCH c.administrador
            WHERE c.ordenServicio.idOrden = :idOrden
            """)
    Optional<LiquidacionEconomica> findByIdOrdenWithCancelacion(@Param("idOrden") Integer idOrden);
}