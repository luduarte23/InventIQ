package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cómo quedó repartido el presupuesto.
 *
 * @param fundedOrderIds   pedidos que caben, en el orden en que se priorizaron
 * @param deferredOrderIds pedidos que no caben y se aplazan
 * @param totalFunded      suma del costo de los pedidos que caben
 */
public record BudgetAllocation(List<Long> fundedOrderIds, List<Long> deferredOrderIds,
                               BigDecimal totalFunded) {

    public BudgetAllocation {
        fundedOrderIds = List.copyOf(fundedOrderIds);
        deferredOrderIds = List.copyOf(deferredOrderIds);
    }
}
