package co.edu.unal.inventiq.application.sales;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** El inventario completo: una fila por producto activo y los indicadores. */
public record InventoryOverview(List<ProductStock> rows, InventorySummary summary) {

    public Map<Long, ProductStock> byProductId() {
        return rows.stream().collect(Collectors.toMap(row -> row.product().id(), Function.identity()));
    }
}
