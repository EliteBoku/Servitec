package com.servitec.servitec_backend.closures.repository;

import com.servitec.servitec_backend.closures.entity.CierreTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CierreTecnicoRepository extends JpaRepository<CierreTecnico, Integer> {

    boolean existsByOrdenServicio_IdOrden(Integer idOrden);

    @Query("""
            SELECT c FROM CierreTecnico c
            JOIN FETCH c.tecnico
            JOIN FETCH c.ordenServicio o
            WHERE o.idOrden = :idOrden
            """)
    Optional<CierreTecnico> findByIdOrdenWithDetails(@Param("idOrden") Integer idOrden);
}