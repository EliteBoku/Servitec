package com.servitec.servitec_backend.identity.repository;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.shared.enums.RolNombre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    @EntityGraph(attributePaths = "rol")
    Optional<Usuario> findWithRolByCorreo(String correo);
    boolean existsByCorreo(String correo);
    boolean existsByRol_Nombre(RolNombre nombre);
}
