package co.edu.unal.inventiq.application.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.model.Product;

/**
 * Precios de compra (costo) y de venta de un producto: los actuales, su historia y cuánto
 * cambiaron en el período.
 *
 * @param history            cambios en orden cronológico
 * @param costChangePercent  variación del costo en el período, o null si no se puede calcular
 * @param priceChangePercent variación del precio de venta en el período
 */
public record ProductPrices(Product product, List<PriceChange> history, BigDecimal costChangePercent,
                            BigDecimal priceChangePercent) {

    /** Fecha del último cambio, o null si no tiene historial. */
    public LocalDateTime lastChange() {
        return history.isEmpty() ? null : history.get(history.size() - 1).changedAt();
    }

    /** Margen actual sobre el precio de venta, en porcentaje con un decimal. */
    public BigDecimal marginPercent() {
        BigDecimal price = product.salePrice();
        if (price.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return price.subtract(product.cost()).multiply(BigDecimal.valueOf(100)).divide(price, 1, RoundingMode.HALF_UP);
    }
}
