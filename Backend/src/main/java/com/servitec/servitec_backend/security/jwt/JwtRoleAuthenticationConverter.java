package com.servitec.servitec_backend.security.jwt;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtRoleAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        String nombreRol = jwt.getClaimAsString("role");

        List<SimpleGrantedAuthority> autoridades =
                nombreRol == null || nombreRol.isBlank()
                        ? List.of()
                        : List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + nombreRol
                        )
                );

        return new JwtAuthenticationToken(
                jwt,
                autoridades,
                jwt.getSubject()
        );
    }
}