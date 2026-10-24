package co.edu.unal.inventiq.application.sales;

import java.util.List;

import co.edu.unal.inventiq.domain.model.ExpirationStatus;
import co.edu.unal.inventiq.domain.model.Product;

/**
 * Fila de la vista de inventario (B5).
 *
 * @param batches           lotes con unidades, en el orden en que se venderán (FIFO)
 * @param sellableUnits     unidades en lotes no vencidos
 * @param expiredUnits      unidades en lotes vencidos, que ya no se venden
 * @param expiringSoonUnits unidades en lotes por vencer (7 días o menos)
 */
public record ProductStock(Product product, List<BatchView> batches, int sellableUnits, int expiredUnits,
                           int expiringSoonUnits) {

    public boolean belowReorderPoint() {
        return product.reorderPoint() != null && sellableUnits <= product.reorderPoint();
    }

    public boolean hasStock() {
        return !batches.isEmpty();
    }

    /**
     * El lote con fecha de vencimiento que hay que atender primero, o null si ningún lote vence.
     * En orden FIFO los vencidos aparecen primero, así que es el más urgente.
     */
    public BatchView nextExpiring() {
        return batches.stream()
                .filter(view -> view.status() != ExpirationStatus.NO_EXPIRATION)
                .findFirst()
                .orElse(null);
    }
}
