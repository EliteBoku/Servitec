package com.servitec.servitec_backend.notifications.event;

import com.servitec.servitec_backend.notifications.service.EmailNotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NotificationEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailNotificationService emailNotificationService;

    public NotificationEventListener(
            EmailNotificationService emailNotificationService
    ) {
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Escucha las solicitudes de activación de cuenta.
     * El envío ocurre después de confirmar la transacción.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejarActivacion(AccountActivationEvent event) {

        try {

            emailNotificationService.enviarCorreoActivacion(event);

            log.info(
                    "Correo de activación enviado correctamente."
            );

        } catch (MailException | IllegalStateException exception) {

            log.error(
                    "No se pudo enviar el correo de activación. Tipo de error: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }

    /**
     * Escucha las solicitudes de recuperación de contraseña.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void manejarRecuperacion(PasswordRecoveryEvent event) {

        try {

            emailNotificationService.enviarCorreoRecuperacion(event);

            log.info(
                    "Correo de recuperación enviado correctamente."
            );

        } catch (MailException | IllegalStateException exception) {

            log.error(
                    "No se pudo enviar el correo de recuperación. Tipo de error: {}",
                    exception.getClass().getSimpleName()
            );
        }
    }
}