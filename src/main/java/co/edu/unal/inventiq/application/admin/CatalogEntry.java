package co.edu.unal.inventiq.application.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;

import co.edu.unal.inventiq.domain.model.Product;

/**
 * Fila del catálogo de productos.
 *
 * @param supplierName null si el producto no tiene proveedor
 */
public record CatalogEntry(Product product, String supplierName) {

    /** Margen sobre el precio de venta, en porcentaje entero: (precio - costo) / precio. */
    public int marginPercent() {
        BigDecimal price = product.salePrice();
        if (price.signum() == 0) {
            return 0;
        }
        return price.subtract(product.cost())
                .multiply(BigDecimal.valueOf(100))
                .divide(price, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
