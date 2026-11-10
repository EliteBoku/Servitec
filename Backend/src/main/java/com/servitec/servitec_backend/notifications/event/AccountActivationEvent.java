package com.servitec.servitec_backend.notifications.event;

/**
 * Evento solicitado cuando se crea una cuenta
 * y debe enviarse su enlace de activación.
 *
 * El token original solo se conserva temporalmente
 * para construir el enlace del correo.
 */
public record AccountActivationEvent(
        String recipientEmail,
        String recipientName,
        String rawToken
) {
}