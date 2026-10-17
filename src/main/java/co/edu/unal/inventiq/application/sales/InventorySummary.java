package co.edu.unal.inventiq.application.sales;

import java.util.List;

import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/** Indicadores de la cabecera del inventario. */
public record InventorySummary(int products, int sellableUnits, int batchesExpiringSoon, int batchesExpired,
                               int pausedProducts, int belowReorderPoint) {

    static InventorySummary of(List<ProductStock> rows) {
        int sellable = 0;
        int expiringSoon = 0;
        int expired = 0;
        int paused = 0;
        int belowReorderPoint = 0;
        for (ProductStock row : rows) {
            sellable += row.sellableUnits();
            for (BatchView view : row.batches()) {
                if (view.status() == ExpirationStatus.EXPIRING_SOON) {
                    expiringSoon++;
                } else if (view.status() == ExpirationStatus.EXPIRED) {
                    expired++;
                }
            }
            if (!row.product().saleEnabled()) {
                paused++;
            }
            if (row.belowReorderPoint()) {
                belowReorderPoint++;
            }
        }
        return new InventorySummary(rows.size(), sellable, expiringSoon, expired, paused, belowReorderPoint);
    }
}
