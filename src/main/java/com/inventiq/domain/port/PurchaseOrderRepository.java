/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// PurchaseOrderRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository {
    Optional<PurchaseOrder> findById(Long id);
    List<PurchaseOrder> findByStatus(OrderStatus status);
    PurchaseOrder save(PurchaseOrder order);
}
