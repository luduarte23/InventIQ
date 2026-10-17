package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;

/**
 * Línea de una venta registrada. Hay una línea por lote descontado: si un producto
 * se cubrió con dos lotes, la venta tiene dos líneas de ese producto.
 */
public record SaleItem(long productId, long batchId, int quantity, BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
