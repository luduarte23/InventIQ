package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula el punto de reorden (ROP) de un producto: el nivel de stock en el que hay que
 * volver a pedir para no quedarse sin unidades mientras llega el pedido, y cuántas
 * unidades pedir. El ROP se guarda en Product, y Product.needsReorder dice si ya toca pedir.
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
     * Cantidad a pedir: lo que se vende mientras llega el pedido y hasta la siguiente
     * revisión, más el stock de seguridad, menos lo que ya hay vendible.
     * Siempre es al menos 1, porque solo se usa cuando el producto ya necesita pedido.
     *
     * @param unitsPerDay velocidad de ventas; vacía cuenta como cero, y entonces se pide
     *                    lo que falta para llegar al stock de seguridad
     */
    public static int suggestedQuantity(Optional<BigDecimal> unitsPerDay, int leadTimeDays,
                                        int reviewPeriodDays, int safetyStock,
                                        int availableStock) {
        Objects.requireNonNull(unitsPerDay, "unitsPerDay es obligatorio");
        requireNonNegative(leadTimeDays, "leadTimeDays");
        requireNonNegative(reviewPeriodDays, "reviewPeriodDays");
        requireNonNegative(safetyStock, "safetyStock");
        requireNonNegative(availableStock, "availableStock");
        int coverage = unitsPerDay.isEmpty()
                ? 0
                : unitsSoldIn(unitsPerDay.get(), leadTimeDays + reviewPeriodDays);
        return Math.max(1, coverage + safetyStock - availableStock);
    }

    /**
     * Unidades que se venden en una cantidad de días, redondeadas hacia arriba.
     * La velocidad llega con 4 decimales (2 unidades en 3 días = 0.6667), así que
     * 0.6667 x 3 da 2.0001. Se deja primero en 2 decimales para que ese residuo
     * del redondeo no sume una unidad que no corresponde.
     * Sin "private": también la usa la regla del pedido mínimo (MoqCompletion).
     */
    static int unitsSoldIn(BigDecimal unitsPerDay, int days) {
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
