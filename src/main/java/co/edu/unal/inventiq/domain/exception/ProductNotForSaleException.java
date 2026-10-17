package co.edu.unal.inventiq.domain.exception;

/** El producto existe y puede tener stock, pero su venta está pausada. */
public class ProductNotForSaleException extends BusinessException {

    public ProductNotForSaleException(String productName) {
        super("«" + productName + "» no está a la venta por ahora. Sigue en el inventario;"
                + " el administrador puede habilitar su venta en Productos.");
    }
}
