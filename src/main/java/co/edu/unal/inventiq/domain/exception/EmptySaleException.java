package co.edu.unal.inventiq.domain.exception;

public class EmptySaleException extends BusinessException {

    public EmptySaleException() {
        super("No hay productos en la venta.");
    }
}
