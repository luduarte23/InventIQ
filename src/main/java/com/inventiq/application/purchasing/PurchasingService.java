/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// PurchasingService.java
package com.inventiq.application.purchasing;

import java.util.List;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Settings;

public interface PurchasingService {

    Settings getSettings();

    Settings updateSettings(Settings newSettings, Long adminUserId);

    void adjustStock(Long productId, Long batchId, int newQuantity, String reason, Long adminUserId);

    List<Batch> batchesOf(Long productId);

    PurchaseOrder updateOrderLines(Long orderId, List<OrderLineEdit> edits);

    PurchaseOrder approveOrder(Long orderId, Long adminUserId);

    SupplierMessage buildSupplierMessage(Long orderId);

    PurchaseOrder sendOrder(Long orderId, Long adminUserId);

    PurchaseOrder confirmReceipt(Long orderId, List<ReceiptLine> lines, Long adminUserId);

    List<PurchaseOrder> ordersAwaitingReception();

    PurchaseOrder getOrder(Long orderId);

    List<PurchaseOrder> allOrders();

    PurchaseOrder closeOrder(Long orderId, Long adminUserId);
}
