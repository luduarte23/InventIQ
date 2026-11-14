package com.inventiq.infrastructure.messaging;

import com.inventiq.domain.port.MessageNotifier;
import com.inventiq.domain.port.NotificationResult;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Notificador falso (A6): en lugar de enviar un WhatsApp, escribe el mensaje en el log.
 * Permite que C, D y el scheduler "envíen" mensajes antes de conectar Twilio.
 *
 * Cumple ACUERDOS §5.6: nunca lanza excepción. Si el número o el mensaje no son válidos,
 * devuelve success = false, igual que haría Twilio, y quien llamó registra la alerta
 * NOTIFICATION_FAILED. Así D puede probar ese camino con un número mal escrito.
 *
 * Es el notificador por defecto (inventiq.notifier=log o sin la propiedad). El adaptador
 * de Twilio usará inventiq.notifier=twilio, y nadie más tendrá que cambiar su código.
 */
@Component
@ConditionalOnProperty(name = "inventiq.notifier", havingValue = "log", matchIfMissing = true)
public class LoggingMessageNotifier implements MessageNotifier {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMessageNotifier.class);

    /** Formato internacional de ACUERDOS §3: "+" y de 8 a 15 dígitos, ej. +573001234567. */
    private static final Pattern WHATSAPP_NUMBER = Pattern.compile("\\+\\d{8,15}");

    @Override
    public NotificationResult send(String whatsappNumber, String body) {
        try {
            if (whatsappNumber == null || !WHATSAPP_NUMBER.matcher(whatsappNumber).matches()) {
                return NotificationResult.failed(
                        "Número de WhatsApp no válido: debe tener el formato +573001234567");
            }
            if (body == null || body.isBlank()) {
                return NotificationResult.failed("El mensaje está vacío");
            }
            String messageId = "log-" + UUID.randomUUID();
            LOG.info("[WhatsApp simulado] id={} para={}\n{}", messageId, mask(whatsappNumber), body);
            return NotificationResult.ok(messageId);
        } catch (RuntimeException e) {
            // Red de seguridad: pase lo que pase, el flujo principal nunca recibe una excepción.
            return NotificationResult.failed("Error inesperado al notificar: " + e.getMessage());
        }
    }

    /** Oculta el centro del número en el log: +573001234567 -> +57******4567. */
    static String mask(String number) {
        int hidden = number.length() - 7;
        return number.substring(0, 3) + "*".repeat(hidden) + number.substring(number.length() - 4);
    }
}
