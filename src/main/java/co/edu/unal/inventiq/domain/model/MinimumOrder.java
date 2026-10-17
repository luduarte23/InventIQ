package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Pedido mínimo de un proveedor para obtener envío gratis o descuento por volumen (MOQ).
 * Lo usa la Entrega 3 (FR-05, US-02).
 *
 * @param value monto en pesos si el tipo es {@link MinimumOrderType#AMOUNT}, o unidades si es
 *              {@link MinimumOrderType#UNITS}
 */
public record MinimumOrder(MinimumOrderType type, BigDecimal value) {

    public MinimumOrder {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("El pedido mínimo debe ser mayor que cero: " + value);
        }
    }

    /** true si un pedido con ese monto y esas unidades alcanza el mínimo. */
    public boolean isMetBy(BigDecimal amount, int units) {
        BigDecimal reached = type == MinimumOrderType.AMOUNT ? amount : BigDecimal.valueOf(units);
        return reached.compareTo(value) >= 0;
    }
}
