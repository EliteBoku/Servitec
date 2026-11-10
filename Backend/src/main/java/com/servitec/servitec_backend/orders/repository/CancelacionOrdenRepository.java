package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.CancelacionOrden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CancelacionOrdenRepository extends JpaRepository<CancelacionOrden, Integer> {

    boolean existsByOrdenServicio_IdOrden(Integer idOrden);

    @Query("""
            SELECT c FROM CancelacionOrden c
            JOIN FETCH c.administrador
            WHERE c.ordenServicio.idOrden = :idOrden
            """)
    Optional<CancelacionOrden> findByIdOrdenWithAdmin(@Param("idOrden") Integer idOrden);
}