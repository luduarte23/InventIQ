package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Precio de venta y costo (precio de compra) de un producto, vigentes desde {@code changedAt}
 * hasta el siguiente cambio.
 *
 * @param id null hasta que se guarda
 */
public record PriceChange(Long id, long productId, BigDecimal salePrice, BigDecimal cost, LocalDateTime changedAt) {

    public PriceChange {
        Objects.requireNonNull(salePrice, "salePrice");
        Objects.requireNonNull(cost, "cost");
        Objects.requireNonNull(changedAt, "changedAt");
    }

    public static PriceChange of(Product product, LocalDateTime changedAt) {
        return new PriceChange(null, product.id(), product.salePrice(), product.cost(), changedAt);
    }
}
