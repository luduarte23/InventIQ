package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Prioriza los borradores de pedido cuando su costo total supera el presupuesto semanal.
 * Primero van los pedidos que más ganancia dejan por día (margen por rotación); los que
 * no caben se aplazan para la siguiente semana.
 */
public class BudgetPrioritizer {

    /**
     * Recorre los pedidos del más rentable al menos rentable y aprueba cada uno si todavía
     * cabe en lo que queda del presupuesto. Si uno grande no cabe, se sigue con los demás:
     * uno más pequeño puede caber todavía.
     */
    public BudgetAllocation prioritize(List<BudgetCandidate> candidates, BigDecimal budget) {
        Objects.requireNonNull(budget, "budget es obligatorio");

        // Más ganancia por día primero; a igual ganancia, por id para que el orden no cambie.
        List<BudgetCandidate> byProfit = new ArrayList<>(candidates);
        byProfit.sort(Comparator.comparing(BudgetCandidate::dailyProfit).reversed()
                .thenComparing(BudgetCandidate::orderId));

        List<Long> funded = new ArrayList<>();
        List<Long> deferred = new ArrayList<>();
        BigDecimal remaining = budget;
        for (BudgetCandidate candidate : byProfit) {
            if (candidate.cost().compareTo(remaining) <= 0) {
                funded.add(candidate.orderId());
                remaining = remaining.subtract(candidate.cost());
            } else {
                deferred.add(candidate.orderId());
            }
        }
        return new BudgetAllocation(funded, deferred, budget.subtract(remaining));
    }

    /**
     * Ganancia por día que deja una línea del pedido: (precio de venta - costo) por unidades
     * vendidas al día. Un producto que se vende a pérdida cuenta como cero.
     */
    public static BigDecimal dailyProfit(BigDecimal salePrice, BigDecimal unitCost,
                                         BigDecimal unitsPerDay) {
        BigDecimal margin = salePrice.subtract(unitCost).max(BigDecimal.ZERO);
        return margin.multiply(unitsPerDay).setScale(2, RoundingMode.HALF_UP);
    }
}
