package com.servitec.servitec_backend.inventory.repository;

import com.servitec.servitec_backend.inventory.entity.Repuesto;
import com.servitec.servitec_backend.shared.enums.TipoItemCatalogo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RepuestoRepository extends JpaRepository<Repuesto, Integer> {

    boolean existsByCodigoRepuestoIgnoreCase(String codigoRepuesto);

    boolean existsByCodigoRepuestoIgnoreCaseAndIdRepuestoNot(String codigoRepuesto, Integer idRepuesto);

    Optional<Repuesto> findByCodigoRepuestoIgnoreCase(String codigoRepuesto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repuesto r WHERE r.idRepuesto = :idRepuesto")
    Optional<Repuesto> findByIdWithLock(@Param("idRepuesto") Integer idRepuesto);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Repuesto r WHERE r.idRepuesto IN :ids ORDER BY r.idRepuesto ASC")
    List<Repuesto> findAllByIdsWithLockOrderById(@Param("ids") Collection<Integer> ids);

    @Query("""
            SELECT r FROM Repuesto r
            WHERE (:busqueda IS NULL OR :busqueda = ''
                   OR LOWER(r.codigoRepuesto) LIKE LOWER(CONCAT('%', :busqueda, '%'))
                   OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :busqueda, '%')))
              AND (:tipoItem IS NULL OR r.tipoItem = :tipoItem)
              AND (:activo IS NULL OR r.activo = :activo)
            ORDER BY r.nombre ASC
            """)
    List<Repuesto> buscarCatalogo(
            @Param("busqueda") String busqueda,
            @Param("tipoItem") TipoItemCatalogo tipoItem,
            @Param("activo") Boolean activo
    );
}