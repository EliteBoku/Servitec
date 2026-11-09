package com.servitec.servitec_backend.identity.bootstrap;

import com.servitec.servitec_backend.identity.entity.Rol;
import com.servitec.servitec_backend.identity.entity.Usuario;
import com.servitec.servitec_backend.identity.repository.RolRepository;
import com.servitec.servitec_backend.identity.repository.UsuarioRepository;
import com.servitec.servitec_backend.shared.enums.EstadoCuenta;
import com.servitec.servitec_backend.shared.enums.RolNombre;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap.admin",
        name = "enabled",
        havingValue = "true"
)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapAdminProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            BootstrapAdminProperties properties,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        if (!properties.isEnabled()) {
            return;
        }

        String nombre = validarTexto(
                properties.getNombre(), "nombre del administrador");

        String correo = validarTexto(
                properties.getCorreo(), "correo del administrador")
                .toLowerCase(Locale.ROOT);

        String contrasena = validarTexto(
                properties.getContrasena(), "contraseña del administrador");

        if (nombre.length() > 150 || correo.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre o correo supera la longitud permitida."
            );
        }

        int longitudBytes = contrasena.getBytes(StandardCharsets.UTF_8).length;

        if (contrasena.length() < 12 || longitudBytes > 72) {
            throw new IllegalArgumentException(
                    "La contraseña inicial debe tener al menos 12 caracteres "
                            + "y no superar 72 bytes UTF-8."
            );
        }

        // Evita crear otro administrador si ya existe uno.
        if (usuarioRepository.existsByRol_Nombre(RolNombre.ADMINISTRADOR)) {
            log.info("Ya existe un administrador. No se creará otro.");
            return;
        }

        // Evita reutilizar el correo de otro usuario.
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new IllegalStateException(
                    "El correo configurado ya está registrado."
            );
        }

        Rol rolAdministrador = rolRepository
                .findByNombre(RolNombre.ADMINISTRADOR)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el rol ADMINISTRADOR en la base de datos."
                ));

        LocalDateTime ahora = LocalDateTime.now(
                ZoneId.of("America/Bogota")
        );

        Usuario administrador = new Usuario(
                rolAdministrador,
                nombre,
                correo,
                passwordEncoder.encode(contrasena),
                EstadoCuenta.ACTIVA,
                ahora,
                ahora
        );

        usuarioRepository.save(administrador);

        log.info("Administrador inicial creado correctamente.");
    }

    private String validarTexto(String valor, String campo) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Falta configurar: " + campo
            );
        }

        return valor.trim();
    }
}