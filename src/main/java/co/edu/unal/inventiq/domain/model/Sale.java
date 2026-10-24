package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Venta cerrada. Se crea solo cuando el stock ya se descontó.
 *
 * @param id null hasta que se guarda
 */
public record Sale(Long id, String cashierUsername, LocalDateTime createdAt, List<SaleItem> items) {

    public Sale {
        Objects.requireNonNull(cashierUsername, "cashierUsername");
        Objects.requireNonNull(createdAt, "createdAt");
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Una venta necesita al menos una línea");
        }
        items = List.copyOf(items);
    }

    public BigDecimal total() {
        return items.stream().map(SaleItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int unitCount() {
        return items.stream().mapToInt(SaleItem::quantity).sum();
    }
}
