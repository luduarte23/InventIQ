package co.edu.unal.inventiq.domain.exception;

/** No hay unidades vendibles (en lotes no vencidos) para cubrir la cantidad pedida. */
public class InsufficientStockException extends BusinessException {

    private final int requested;
    private final int available;

    public InsufficientStockException(String productName, int requested, int available) {
        super("Stock insuficiente de " + productName + ": se piden " + requested
                + " y hay " + available + " disponibles.");
        this.requested = requested;
        this.available = available;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
