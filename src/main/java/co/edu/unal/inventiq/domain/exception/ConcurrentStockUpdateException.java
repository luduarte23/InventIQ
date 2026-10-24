package co.edu.unal.inventiq.domain.exception;

/**
 * Otro proceso modificó los mismos lotes al mismo tiempo (bloqueo optimista).
 * La infraestructura traduce su excepción técnica a esta, y SalesService reintenta.
 */
public class ConcurrentStockUpdateException extends BusinessException {

    public ConcurrentStockUpdateException(Throwable cause) {
        super("El inventario cambió mientras se cerraba la venta. Intenta cerrarla de nuevo.");
        initCause(cause);
    }
}
