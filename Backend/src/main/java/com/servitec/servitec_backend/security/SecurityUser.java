package com.servitec.servitec_backend.security;

import com.servitec.servitec_backend.identity.entity.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {

    private final Integer idUsuario;
    private final String correo;
    private final String contrasenaHash;
    private final String rol;
    private final boolean enabled;

    public SecurityUser(
            Integer idUsuario,
            String correo,
            String contrasenaHash,
            String rol,
            boolean enabled
    ) {
        this.idUsuario = idUsuario;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.rol = rol;
        this.enabled = enabled;
    }

    public static SecurityUser from(Usuario usuario) {

        boolean cuentaActiva =
                usuario.getEstadoCuenta() != null
                        && "ACTIVA".equals(usuario.getEstadoCuenta().name());

        boolean tieneContrasena =
                usuario.getContrasenaHash() != null
                        && !usuario.getContrasenaHash().isBlank();

        return new SecurityUser(
                usuario.getIdUsuario(),
                usuario.getCorreo(),
                usuario.getContrasenaHash(),
                usuario.getRol().getNombre().name(),
                cuentaActiva && tieneContrasena
        );
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public String getRol() {
        return rol;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + rol)
        );
    }

    @Override
    public String getPassword() {
        return contrasenaHash == null ? "" : contrasenaHash;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}