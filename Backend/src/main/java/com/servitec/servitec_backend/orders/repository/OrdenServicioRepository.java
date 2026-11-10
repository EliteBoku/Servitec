package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.OrdenServicio;
import com.servitec.servitec_backend.shared.enums.EstadoOperativoOrden;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o FROM OrdenServicio o
            JOIN FETCH o.cliente
            JOIN FETCH o.categoria
            LEFT JOIN FETCH o.tecnicoAsignado
            WHERE o.idOrden = :idOrden
            """)
    Optional<OrdenServicio> findByIdWithLock(@Param("idOrden") Integer idOrden);

    @Query("""
            SELECT o FROM OrdenServicio o
            JOIN FETCH o.cliente
            JOIN FETCH o.categoria
            LEFT JOIN FETCH o.tecnicoAsignado
            WHERE o.idOrden = :idOrden
            """)
    Optional<OrdenServicio> findByIdWithDetails(@Param("idOrden") Integer idOrden);

    @Query("""
            SELECT o FROM OrdenServicio o
            JOIN FETCH o.cliente c
            JOIN FETCH o.categoria cat
            LEFT JOIN FETCH o.tecnicoAsignado t
            WHERE c.idUsuario = :idCliente
              AND (:estado IS NULL OR o.estadoOperativo = :estado)
            ORDER BY o.fechaSolicitud DESC, o.idOrden DESC
            """)
    List<OrdenServicio> findAllByClienteId(
            @Param("idCliente") Integer idCliente,
            @Param("estado") EstadoOperativoOrden estado
    );

    @Query("""
            SELECT o FROM OrdenServicio o
            JOIN FETCH o.cliente c
            JOIN FETCH o.categoria cat
            JOIN FETCH o.tecnicoAsignado t
            WHERE t.idUsuario = :idTecnico
              AND (:estado IS NULL OR o.estadoOperativo = :estado)
            ORDER BY o.fechaEstimada ASC, o.fechaSolicitud DESC
            """)
    List<OrdenServicio> findAllByTecnicoAsignadoId(
            @Param("idTecnico") Integer idTecnico,
            @Param("estado") EstadoOperativoOrden estado
    );

    @Query("""
            SELECT o FROM OrdenServicio o
            JOIN FETCH o.cliente c
            JOIN FETCH o.categoria cat
            LEFT JOIN FETCH o.tecnicoAsignado t
            WHERE (:estado IS NULL OR o.estadoOperativo = :estado)
              AND (:idCategoria IS NULL OR cat.idCategoria = :idCategoria)
              AND (:idTecnico IS NULL OR t.idUsuario = :idTecnico)
              AND (:busqueda IS NULL OR :busqueda = ''
                   OR LOWER(o.codigoOrden) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(c.nombreCompleto) LIKE LOWER(CONCAT('%', :busqueda, '%')))
            ORDER BY o.fechaSolicitud DESC, o.idOrden DESC
            """)
    List<OrdenServicio> buscarOrdenesAdmin(
            @Param("estado") EstadoOperativoOrden estado,
            @Param("idCategoria") Integer idCategoria,
            @Param("idTecnico") Integer idTecnico,
            @Param("busqueda") String busqueda
    );
}