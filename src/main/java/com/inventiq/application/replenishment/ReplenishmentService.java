/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// ReplenishmentService.java
package com.inventiq.application.replenishment;

import com.inventiq.domain.model.PurchaseOrder;
import java.math.BigDecimal;
import java.util.List;

public interface ReplenishmentService {

    /** FR-03: actualiza Product.reorderPoint. Devuelve los ids de los productos actualizados. */
    List<Long> recalculateReorderPoints();

    /** FR-04: un DRAFT por proveedor + uno "por revisar". También lo invoca el scheduler. */
    List<PurchaseOrder> generateDraftOrders();

    /** FR-05: solo sugiere. Para aceptar, la UI llama a PurchasingService.updateOrderLines (§6.4.1). */
    List<OrderLineSuggestion> suggestMoqCompletion(Long orderId);

    /** FR-06 */
    BudgetResult prioritizeUnderBudget(List<Long> draftOrderIds, BigDecimal availableBudget);
}
