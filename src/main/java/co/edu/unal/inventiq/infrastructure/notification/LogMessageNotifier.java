package co.edu.unal.inventiq.infrastructure.notification;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.port.MessageNotifier;

/**
 * Notificador de la Entrega 2: en lugar de enviar el mensaje por WhatsApp, lo escribe en el log.
 * Así la app arranca sin credenciales ni variables de entorno. TwilioMessageNotifier lo reemplaza
 * en la Entrega 3, solo si hay credenciales.
 *
 * <p>Se comporta como lo haría el proveedor real: si falta el número o no tiene formato
 * internacional, el mensaje no "sale" y devuelve false, para que quien llama cree la alerta.
 */
@Component
public class LogMessageNotifier implements MessageNotifier {

    private static final Logger log = LoggerFactory.getLogger(LogMessageNotifier.class);

    /** Formato internacional E.164 que exige WhatsApp: + y de 8 a 15 dígitos. */
    private static final Pattern INTERNATIONAL_NUMBER = Pattern.compile("\\+\\d{8,15}");

    private static final int VISIBLE_DIGITS = 4;

    @Override
    public boolean send(String recipient, String message) {
        if (message == null || message.isBlank()) {
            log.warn("[WhatsApp simulado] Mensaje no enviado: el mensaje está vacío.");
            return false;
        }
        if (recipient == null || recipient.isBlank()) {
            log.warn("[WhatsApp simulado] Mensaje no enviado: no hay un número de WhatsApp configurado.\n{}", message);
            return false;
        }
        String number = recipient.strip();
        if (!INTERNATIONAL_NUMBER.matcher(number).matches()) {
            log.warn("[WhatsApp simulado] Mensaje no enviado: el número {} no tiene formato internacional "
                    + "(p. ej. +573001234567).\n{}", mask(number), message);
            return false;
        }
        log.info("[WhatsApp simulado] Mensaje para {}:\n{}", mask(number), message);
        return true;
    }

    /** Oculta el número en el log y deja ver solo los últimos dígitos: +573001234567 → +********4567. */
    static String mask(String number) {
        int hidden = Math.max(0, number.length() - VISIBLE_DIGITS);
        StringBuilder masked = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            masked.append(i < hidden && Character.isDigit(c) ? '*' : c);
        }
        return masked.toString();
    }
}
