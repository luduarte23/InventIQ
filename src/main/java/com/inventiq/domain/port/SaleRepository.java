/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// SaleRepository.java
package com.inventiq.domain.port;

import com.inventiq.domain.model.Sale;
import java.time.Instant;
import java.util.Optional;

public interface SaleRepository {
    Sale save(Sale sale);
    /** Unidades vendidas del producto desde 'since' (para la velocidad de ventas, §5.1). */
    int unitsSoldSince(Long productId, Instant since);

    // Propuesta A2: la necesitan SalesService.scan(saleId, ...) y finishSale(saleId) (§6.2).
    Optional<Sale> findById(Long id);
}
