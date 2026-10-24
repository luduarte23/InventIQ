package co.edu.unal.inventiq.application.sales;

import java.util.List;

/** El inventario completo: una fila por producto activo y los indicadores. */
public record InventoryOverview(List<ProductStock> rows, InventorySummary summary) {
}
