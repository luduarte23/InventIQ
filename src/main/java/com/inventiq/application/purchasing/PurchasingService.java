/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// PurchasingService.java
package com.inventiq.application.purchasing;

import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Settings;
import java.util.List;

public interface PurchasingService {

    Settings getSettings();

    /** FR-08 */
    Settings updateSettings(Settings newSettings, Long adminUserId);

    /** FR-08 */
    void adjustStock(Long productId, Long batchId, int newQuantity, String reason, Long adminUserId);

    /** Solo en DRAFT. */
    PurchaseOrder updateOrderLines(Long orderId, List<OrderLineEdit> edits);

    /** DRAFT -> APPROVED (NFR-02: máximo 3 clics). */
    PurchaseOrder approveOrder(Long orderId, Long adminUserId);

    /** FR-09 */
    SupplierMessage buildSupplierMessage(Long orderId);

    /** APPROVED -> SENT. Usa MessageNotifier. */
    PurchaseOrder sendOrder(Long orderId, Long adminUserId);

    /** FR-10 */
    PurchaseOrder confirmReceipt(Long orderId, List<ReceiptLine> lines, Long adminUserId);
}
