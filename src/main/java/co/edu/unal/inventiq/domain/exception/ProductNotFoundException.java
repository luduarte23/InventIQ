package co.edu.unal.inventiq.domain.exception;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(long productId) {
        super("El producto " + productId + " no existe.");
    }
}
