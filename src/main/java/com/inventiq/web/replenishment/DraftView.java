package com.inventiq.web.replenishment;

import com.inventiq.application.replenishment.OrderLineSuggestion;
import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Un borrador listo para mostrar: proveedor, líneas en texto, total y sugerencias de pedido mínimo. */
public record DraftView(String supplier, List<String> lines, BigDecimal total, List<String> moqSuggestions) {

    /** Arma la vista con los nombres que trae el resumen de productos (por id). */
    static DraftView of(PurchaseOrder order, Map<Long, ReorderStatus> rows, List<OrderLineSuggestion> moq) {
        String supplier = order.isNeedsReview() ? "Por revisar (sin proveedor)"
                : rows.get(order.getLines().get(0).getProductId()).supplierName();
        List<String> lines = order.getLines().stream()
                .map(l -> l.getQuantityOrdered() + " x " + rows.get(l.getProductId()).name()).toList();
        List<String> suggestions = moq.stream().map(s -> s.suggestedQuantity() + " x "
                + rows.get(s.productId()).name() + " por $ " + s.lineCost()).toList();
        return new DraftView(supplier, lines, order.getTotalCost(), suggestions);
    }
}
