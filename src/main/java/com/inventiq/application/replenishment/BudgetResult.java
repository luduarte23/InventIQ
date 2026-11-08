/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// BudgetResult.java
package com.inventiq.application.replenishment;

import java.math.BigDecimal;
import java.util.List;

public record BudgetResult(List<Long> fundedOrderIds, List<Long> deferredOrderIds,
                           BigDecimal totalFunded) {

    public BudgetResult {
        fundedOrderIds = List.copyOf(fundedOrderIds);
        deferredOrderIds = List.copyOf(deferredOrderIds);
    }
}
