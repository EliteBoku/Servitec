package com.servitec.servitec_backend.diagnostics.repository;

import com.servitec.servitec_backend.diagnostics.entity.Diagnostico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DiagnosticoRepository extends JpaRepository<Diagnostico, Integer> {

    boolean existsByOrdenServicio_IdOrden(Integer idOrden);

    @Query("""
            SELECT d FROM Diagnostico d
            JOIN FETCH d.ordenServicio o
            JOIN FETCH o.categoria
            JOIN FETCH d.tecnico
            WHERE o.idOrden = :idOrden
            """)
    Optional<Diagnostico> findByIdOrdenWithDetails(@Param("idOrden") Integer idOrden);
}