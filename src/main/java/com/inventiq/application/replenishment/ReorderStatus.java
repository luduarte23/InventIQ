package com.inventiq.application.replenishment;

import java.math.BigDecimal;

/**
 * Una fila de la pantalla de reabastecimiento.
 *
 * @param unitsPerDay       velocidad de ventas; null si el producto no tiene historial
 * @param suggestedQuantity cuánto pedir; 0 si todavía no necesita pedido
 */
public record ReorderStatus(Long productId, String barcode, String name, String supplierName,
                            int availableStock, BigDecimal unitsPerDay, int reorderPoint,
                            boolean needsReorder, int suggestedQuantity) {
}
