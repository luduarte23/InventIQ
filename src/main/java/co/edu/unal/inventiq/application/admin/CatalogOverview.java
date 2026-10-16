package co.edu.unal.inventiq.application.admin;

import java.util.List;

/**
 * El catálogo y sus indicadores.
 *
 * @param averageMarginPercent margen promedio de los productos, o null si no hay productos
 */
public record CatalogOverview(List<CatalogEntry> entries, int onSale, int paused, int withoutSupplier,
                              Integer averageMarginPercent) {

    static CatalogOverview of(List<CatalogEntry> entries) {
        int onSale = (int) entries.stream().filter(entry -> entry.product().saleEnabled()).count();
        int withoutSupplier = (int) entries.stream().filter(entry -> entry.product().supplierId() == null).count();
        Integer averageMargin = entries.isEmpty() ? null
                : (int) Math.round(entries.stream().mapToInt(CatalogEntry::marginPercent).average().orElse(0));
        return new CatalogOverview(entries, onSale, entries.size() - onSale, withoutSupplier, averageMargin);
    }

    public int total() {
        return entries.size();
    }
}
