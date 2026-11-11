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
public class OrderCancellationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationNotificationListener.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${spring.mail.username:no-reply@servitec.local}")
    private String remitente;

    public OrderCancellationNotificationListener(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        String enlaceLiquidacion = String.format(
                "%s/cliente/ordenes/%d/liquidacion",
                frontendUrl, event.idOrden()
        );

        String asunto = "Servitec - Orden de servicio cancelada (" + event.codigoOrden() + ")";
        String cuerpo = String.format(
                """
                Hola, %s:
                
                Te informamos que tu orden de servicio %s ha sido cancelada.
                Motivo registrado: %s
                
                Se ha generado la liquidación económica informativa por los trabajos y elementos autorizados efectivamente utilizados hasta el momento de la cancelación (Valor total calculado: $%s COP).
                
                Puedes consultar el detalle de la liquidación informativa ingresando con tu cuenta en:
                %s
                
                Atentamente,
                Equipo Servitec Los Alpes
                """,
                event.nombreCliente(),
                event.codigoOrden(),
                event.justificacionCancelacion(),
                event.totalLiquidacionCalculado().stripTrailingZeros().toPlainString(),
                enlaceLiquidacion
        );

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(event.correoCliente());
            mensaje.setSubject(asunto);
            mensaje.setText(cuerpo);
            mailSender.send(mensaje);
            log.info("Notificación de cancelación de orden {} enviada a {}. Enlace: {}",
                    event.codigoOrden(), event.correoCliente(), enlaceLiquidacion);
        } catch (Exception ex) {
            log.warn("No fue posible enviar el correo SMTP de cancelación para la orden {} (Enlace: {}): {}",
                    event.codigoOrden(), enlaceLiquidacion, ex.getMessage());
        }
    }
}