package com.servitec.servitec_backend.notifications.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ComplementaryBudgetNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ComplementaryBudgetNotificationListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@servitec.local}")
    private String remitente;

    public ComplementaryBudgetNotificationListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onComplementaryBudgetCreated(ComplementaryBudgetCreatedEvent event) {
        String enlaceSeguro = String.format(
                "%s/cliente/ordenes/%d/presupuestos/%d",
                frontendUrl, event.idOrden(), event.idPresupuesto()
        );

        String asunto = event.esNuevaPropuestaAutorizada()
                ? "Servitec - Nueva propuesta de presupuesto complementario (" + event.codigoPresupuesto() + ")"
                : "Servitec - Presupuesto complementario pendiente de aprobación (" + event.codigoPresupuesto() + ")";

        String cuerpo = String.format(
                """
                Hola, %s:
                
                Se ha generado un presupuesto complementario (%s) asociado a tu orden de servicio %s por un valor adicional estimado de $%s COP.
                
                Para revisar el detalle de los repuestos e insumos adicionales y aprobar o rechazar digitalmente esta cotización, ingresa con tu cuenta en el siguiente enlace seguro:
                %s
                
                Atentamente,
                Equipo Servitec Los Alpes
                """,
                event.nombreCliente(),
                event.codigoPresupuesto(),
                event.codigoOrden(),
                event.totalAdicional().stripTrailingZeros().toPlainString(),
                enlaceSeguro
        );

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(event.correoCliente());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("Notificación de presupuesto complementario {} enviada a {}. Enlace: {}",
                    event.codigoPresupuesto(), event.correoCliente(), enlaceSeguro);
        } catch (Exception ex) {
            log.warn("No fue posible enviar el correo SMTP para el presupuesto complementario {} (Enlace seguro generado: {}): {}",
                    event.codigoPresupuesto(), enlaceSeguro, ex.getMessage());
        }
    }
}