package com.servitec.servitec_backend.security;

import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {

        Usuario usuario = usuarioRepository.findWithRolByCorreo(
                correo.trim()
        ).orElseThrow(() ->
                new UsernameNotFoundException("Credenciales inválidas.")
        );

        return SecurityUser.from(usuario);
    }
}