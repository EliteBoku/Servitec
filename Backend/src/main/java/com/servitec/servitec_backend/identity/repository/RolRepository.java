package com.servitec.servitec_backend.identity.repository;

import com.servitec.servitec_backend.identity.entity.Rol;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(RolNombre nombre);
}