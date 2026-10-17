package co.edu.unal.inventiq.domain.port;

/**
 * Envío de mensajes por WhatsApp (en la Entrega 1, NotificadorMensajes). En la Entrega 2 solo
 * existe una implementación que escribe en el log; Twilio llega en la Entrega 3.
 */
public interface MessageNotifier {

    /**
     * Envía un mensaje. Nunca lanza excepciones: una falla del proveedor no debe cortar el flujo
     * principal, así que el llamador decide qué hacer (p. ej. crear una alerta NOTIFICATION_FAILED).
     *
     * @param recipient número de WhatsApp en formato internacional, p. ej. +573001234567
     * @return true si el mensaje se entregó al proveedor de mensajería
     */
    boolean send(String recipient, String message);
}
