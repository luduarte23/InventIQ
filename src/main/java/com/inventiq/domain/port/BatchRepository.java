/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// BatchRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.Batch;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BatchRepository {
    /** Lotes no vencidos con unidades, ya ordenados FIFO (§5.3). */
    List<Batch> findAvailableByProduct(Long productId, LocalDate today);
    /** Suma de quantityRemaining de lotes no vencidos (§5.2). */
    int availableStock(Long productId, LocalDate today);
    List<Batch> findExpiringBefore(LocalDate limit);
    Batch save(Batch batch);

    // Propuesta A2: la necesitan DiscountService.setManualDiscount(batchId, ...) (§6.3)
    // y PurchasingService.adjustStock(productId, batchId, ...) (§6.5).
    Optional<Batch> findById(Long id);
}
