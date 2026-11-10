package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.ConsentimientoDatos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimientoDatosRepository extends JpaRepository<ConsentimientoDatos, Integer> {

    boolean existsByCliente_IdUsuario(Integer idCliente);

    Optional<ConsentimientoDatos> findByCliente_IdUsuario(Integer idCliente);
}