package com.servitec.servitec_backend.notifications.service;

import com.servitec.servitec_backend.notifications.config.NotificationProperties;
import com.servitec.servitec_backend.notifications.event.AccountActivationEvent;
import com.servitec.servitec_backend.notifications.event.PasswordRecoveryEvent;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationProperties properties;

    public EmailNotificationService(
            JavaMailSender mailSender,
            NotificationProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    /**
     * Envía el correo para activar una cuenta.
     */
    public void enviarCorreoActivacion(AccountActivationEvent event) {

        String enlace = construirEnlace(
                properties.getActivationPath(),
                event.rawToken()
        );

        String nombre = obtenerNombre(event.recipientName());

        String contenido = """
                Hola %s,

                Se ha creado una cuenta para ti en Servitec Los Alpes.

                Para activar tu cuenta y establecer tu contraseña inicial,
                ingresa al siguiente enlace:

                %s

                Este enlace es de un solo uso y tiene vigencia limitada.

                Si no esperabas este mensaje, puedes ignorarlo.

                Atentamente,
                Equipo Servitec Los Alpes
                """.formatted(nombre, enlace);

        enviarCorreo(
                event.recipientEmail(),
                "Activa tu cuenta de Servitec",
                contenido
        );
    }

    /**
     * Envía el correo para recuperar una contraseña.
     */
    public void enviarCorreoRecuperacion(PasswordRecoveryEvent event) {

        String enlace = construirEnlace(
                properties.getRecoveryPath(),
                event.rawToken()
        );

        String nombre = obtenerNombre(event.recipientName());

        String contenido = """
                Hola %s,

                Hemos recibido una solicitud para restablecer tu contraseña
                de Servitec Los Alpes.

                Para establecer una nueva contraseña, ingresa al siguiente enlace:

                %s

                Este enlace es de un solo uso y tiene vigencia limitada.

                Si no solicitaste el restablecimiento de tu contraseña,
                puedes ignorar este mensaje.

                Atentamente,
                Equipo Servitec Los Alpes
                """.formatted(nombre, enlace);

        enviarCorreo(
                event.recipientEmail(),
                "Recuperación de contraseña - Servitec",
                contenido
        );
    }

    /**
     * Construye el enlace del frontend con el token como parámetro.
     */
    private String construirEnlace(String ruta, String token) {

        validarConfiguracion();

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "El token de notificación es obligatorio."
            );
        }

        String baseUrl = properties.getFrontendBaseUrl()
                .replaceAll("/+$", "");

        String rutaNormalizada = ruta.startsWith("/")
                ? ruta
                : "/" + ruta;

        String tokenCodificado = URLEncoder.encode(
                token,
                StandardCharsets.UTF_8
        );

        return baseUrl
                + rutaNormalizada
                + "?token="
                + tokenCodificado;
    }

    /**
     * Envía un correo electrónico de texto plano.
     */
    private void enviarCorreo(
            String destinatario,
            String asunto,
            String contenido
    ) {

        validarConfiguracion();

        if (destinatario == null || destinatario.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo destinatario es obligatorio."
            );
        }

        SimpleMailMessage mensaje = new SimpleMailMessage();

        mensaje.setFrom(properties.getFrom().trim());
        mensaje.setTo(destinatario.trim());
        mensaje.setSubject(asunto);
        mensaje.setText(contenido);

        mailSender.send(mensaje);
    }

    /**
     * Verifica que exista la configuración necesaria.
     */
    private void validarConfiguracion() {

        if (properties.getFrom() == null
                || properties.getFrom().isBlank()) {

            throw new IllegalStateException(
                    "No se ha configurado el correo remitente de Servitec."
            );
        }

        if (properties.getFrontendBaseUrl() == null
                || properties.getFrontendBaseUrl().isBlank()) {

            throw new IllegalStateException(
                    "No se ha configurado la URL base del frontend."
            );
        }
    }

    private String obtenerNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return "Usuario";
        }

        return nombre.trim();
    }
}