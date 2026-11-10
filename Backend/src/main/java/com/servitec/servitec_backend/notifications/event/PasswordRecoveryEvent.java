package com.servitec.servitec_backend.notifications.event;

/**
 * Evento solicitado cuando un usuario requiere
 * recuperar su contraseña mediante correo.
 */
public record PasswordRecoveryEvent(
        String recipientEmail,
        String recipientName,
        String rawToken
) {
}