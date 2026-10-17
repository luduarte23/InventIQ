package co.edu.unal.inventiq.domain.exception;

public class InvalidQuantityException extends BusinessException {

    public InvalidQuantityException(int quantity) {
        super("La cantidad debe ser mayor que cero (se recibió " + quantity + ").");
    }
}
