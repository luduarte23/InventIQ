package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula el punto de reorden (ROP) de un producto: el nivel de stock en el que hay que
 * volver a pedir para no quedarse sin unidades mientras llega el pedido.
 * El resultado se guarda en Product, y Product.needsReorder dice si ya toca pedir.
 */
public final class ReorderPointCalculator {

    private ReorderPointCalculator() {
        // Solo tiene métodos estáticos.
    }

    /**
     * ROP = unidades que se venden mientras llega el pedido + stock de seguridad.
     * Las unidades se redondean hacia arriba: si se venden 1.5, hay que cubrir 2.
     *
     * @param unitsPerDay velocidad de ventas; vacía si el producto no tiene historial,
     *                    y entonces el ROP es solo el stock de seguridad
     */
    public static int reorderPoint(Optional<BigDecimal> unitsPerDay, int leadTimeDays,
                                   int safetyStock) {
        Objects.requireNonNull(unitsPerDay, "unitsPerDay es obligatorio");
        requireNonNegative(leadTimeDays, "leadTimeDays");
        requireNonNegative(safetyStock, "safetyStock");
        if (unitsPerDay.isEmpty()) {
            return safetyStock;
        }
        return unitsSoldIn(unitsPerDay.get(), leadTimeDays) + safetyStock;
    }

    /**
     * Unidades que se venden en una cantidad de días, redondeadas hacia arriba.
     * La velocidad llega con 4 decimales (2 unidades en 3 días = 0.6667), así que
     * 0.6667 x 3 da 2.0001. Se deja primero en 2 decimales para que ese residuo
     * del redondeo no sume una unidad que no corresponde.
     */
    private static int unitsSoldIn(BigDecimal unitsPerDay, int days) {
        BigDecimal units = unitsPerDay.multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
        return units.setScale(0, RoundingMode.CEILING).intValueExact();
    }

    private static void requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
    }
}
