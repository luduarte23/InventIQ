package co.edu.unal.inventiq.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;

/**
 * Venta en curso: lo que el cajero lleva escaneado. No toca el stock; el stock se
 * descuenta al cerrar la venta. Así una venta abandonada no deja unidades apartadas.
 * Escanear dos veces el mismo producto suma la cantidad en la misma línea.
 */
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<Long, CartLine> lines = new LinkedHashMap<>();

    public void add(Product product, int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        lines.merge(product.id(),
                new CartLine(product.id(), product.barcode(), product.name(), product.salePrice(), quantity),
                (current, added) -> current.withQuantity(current.quantity() + added.quantity()));
    }

    public void remove(long productId) {
        lines.remove(productId);
    }

    public int quantityOf(long productId) {
        CartLine line = lines.get(productId);
        return line == null ? 0 : line.quantity();
    }

    public List<CartLine> lines() {
        return List.copyOf(lines.values());
    }

    public BigDecimal total() {
        return lines.values().stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int unitCount() {
        return lines.values().stream().mapToInt(CartLine::quantity).sum();
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /** Precio tomado al escanear: es el que se cobra al cerrar. */
    public record CartLine(long productId, String barcode, String productName, BigDecimal unitPrice, int quantity)
            implements Serializable {

        public CartLine withQuantity(int newQuantity) {
            return new CartLine(productId, barcode, productName, unitPrice, newQuantity);
        }

        public BigDecimal subtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
