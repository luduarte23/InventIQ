package co.edu.unal.inventiq.fakes;

import java.util.ArrayList;
import java.util.List;

import co.edu.unal.inventiq.domain.port.MessageNotifier;

/**
 * Notificador para probar servicios sin Spring: guarda los mensajes en memoria y se puede poner a
 * fallar para probar el camino de la alerta NOTIFICATION_FAILED.
 */
public class FakeMessageNotifier implements MessageNotifier {

    public record SentMessage(String recipient, String message) {
    }

    private final List<SentMessage> sent = new ArrayList<>();
    private boolean failing;

    @Override
    public boolean send(String recipient, String message) {
        if (failing) {
            return false;
        }
        sent.add(new SentMessage(recipient, message));
        return true;
    }

    /** A partir de ahora, todos los envíos fallan (o vuelven a funcionar con false). */
    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public List<SentMessage> sent() {
        return List.copyOf(sent);
    }
}
