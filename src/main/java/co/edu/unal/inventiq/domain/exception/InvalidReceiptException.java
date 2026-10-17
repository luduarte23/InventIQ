package co.edu.unal.inventiq.domain.exception;

/** La recepción de un pedido no cuadra con lo pedido (US-05). */
public class InvalidReceiptException extends BusinessException {

    public InvalidReceiptException(String message) {
        super(message);
    }
}
