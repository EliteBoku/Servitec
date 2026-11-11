package com.servitec.servitec_backend.execution.repository;

import com.servitec.servitec_backend.execution.entity.RegistroTiempo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistroTiempoRepository extends JpaRepository<RegistroTiempo, Integer> {

    boolean existsByOrdenServicio_IdOrdenAndFechaHoraFinIsNull(Integer idOrden);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM RegistroTiempo r
            JOIN FETCH r.tecnico
            WHERE r.ordenServicio.idOrden = :idOrden
              AND r.fechaHoraFin IS NULL
            """)
    Optional<RegistroTiempo> findIntervaloAbiertoByOrdenWithLock(@Param("idOrden") Integer idOrden);

    @Query("""
            SELECT r FROM RegistroTiempo r
            JOIN FETCH r.tecnico
            WHERE r.ordenServicio.idOrden = :idOrden
            ORDER BY r.fechaHoraInicio ASC, r.idRegistroTiempo ASC
            """)
    List<RegistroTiempo> findAllByIdOrdenWithTecnicoOrderByInicioAsc(@Param("idOrden") Integer idOrden);

    @Query("""
            SELECT COALESCE(SUM(r.duracionSegundos), 0)
            FROM RegistroTiempo r
            WHERE r.ordenServicio.idOrden = :idOrden
              AND r.duracionSegundos IS NOT NULL
            """)
    Long sumSegundosEfectivosCerradosByOrden(@Param("idOrden") Integer idOrden);
}