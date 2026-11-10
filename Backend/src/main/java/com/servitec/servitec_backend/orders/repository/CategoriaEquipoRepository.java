package com.servitec.servitec_backend.orders.repository;

import com.servitec.servitec_backend.orders.entity.CategoriaEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaEquipoRepository extends JpaRepository<CategoriaEquipo, Integer> {

    List<CategoriaEquipo> findByActivaTrueOrderByNombreAsc();

    Optional<CategoriaEquipo> findByIdCategoriaAndActivaTrue(Integer idCategoria);
}