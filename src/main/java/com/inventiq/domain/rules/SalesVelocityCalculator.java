package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Calcula cuántas unidades de un producto se venden por día.
 * Es la única clase que hace este cálculo: el reabastecimiento y los descuentos la usan.
 */
public final class SalesVelocityCalculator {

    /** Decimales del resultado: 2 unidades en 3 días dan 0.6667 por día. */
    private static final int SCALE = 4;

    private SalesVelocityCalculator() {
        // Solo tiene métodos estáticos.
    }

    /**
     * Unidades vendidas en la ventana divididas por los días de la ventana.
     * Los días sin ventas también cuentan: 30 unidades en 30 días son 1 por día,
     * aunque todas se hayan vendido el mismo día.
     *
     * @return vacío si no hubo ventas. Vacío no es cero: significa que no hay historial
     *         y quien llama decide qué hacer en ese caso.
     */
    public static Optional<BigDecimal> unitsPerDay(int unitsSoldInWindow, int windowDays) {
        if (windowDays <= 0) {
            throw new IllegalArgumentException("windowDays debe ser mayor que 0");
        }
        if (unitsSoldInWindow < 0) {
            throw new IllegalArgumentException("unitsSoldInWindow no puede ser negativo");
        }
        if (unitsSoldInWindow == 0) {
            return Optional.empty();
        }
        BigDecimal velocity = BigDecimal.valueOf(unitsSoldInWindow)
                .divide(BigDecimal.valueOf(windowDays), SCALE, RoundingMode.HALF_UP);
        return Optional.of(velocity);
    }
}
