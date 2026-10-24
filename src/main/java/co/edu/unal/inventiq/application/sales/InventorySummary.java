package co.edu.unal.inventiq.application.sales;

import java.util.List;

import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/** Indicadores de la cabecera del inventario. */
public record InventorySummary(int products, int sellableUnits, int batchesExpiringSoon, int batchesExpired,
                               int belowReorderPoint) {

    static InventorySummary of(List<ProductStock> rows) {
        int sellable = 0;
        int expiringSoon = 0;
        int expired = 0;
        int belowReorderPoint = 0;
        for (ProductStock row : rows) {
            sellable += row.sellableUnits();
            expiringSoon += (int) row.batches().stream().filter(b -> b.status() == ExpirationStatus.EXPIRING_SOON).count();
            expired += (int) row.batches().stream().filter(b -> b.status() == ExpirationStatus.EXPIRED).count();
            belowReorderPoint += row.belowReorderPoint() ? 1 : 0;
        }
        return new InventorySummary(rows.size(), sellable, expiringSoon, expired, belowReorderPoint);
    }
}
