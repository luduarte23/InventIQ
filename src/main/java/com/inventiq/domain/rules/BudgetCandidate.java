package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Borrador de pedido que compite por el presupuesto semanal.
 *
 * @param cost        lo que cuesta el pedido completo
 * @param dailyProfit ganancia que dejan por día los productos del pedido, según su margen y
 *                    lo que se venden; sirve para decidir qué pedido va primero
 */
public record BudgetCandidate(Long orderId, BigDecimal cost, BigDecimal dailyProfit) {

    public BudgetCandidate {
        Objects.requireNonNull(orderId, "orderId es obligatorio");
        Objects.requireNonNull(cost, "cost es obligatorio");
        Objects.requireNonNull(dailyProfit, "dailyProfit es obligatorio");
    }
}
