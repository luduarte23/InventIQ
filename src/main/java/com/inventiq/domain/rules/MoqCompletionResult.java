package com.inventiq.domain.rules;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de intentar completar el pedido mínimo.
 *
 * @param suggestions     líneas sugeridas, de mayor a menor rotación; vacía si no hace falta
 *                        agregar nada o si no alcanza el presupuesto
 * @param blockedByBudget true si faltaba llegar al mínimo pero el presupuesto no alcanza:
 *                        el administrador decide si compra igual el borrador o lo aplaza
 */
public record MoqCompletionResult(List<MoqSuggestion> suggestions, boolean blockedByBudget) {

    public MoqCompletionResult {
        suggestions = List.copyOf(suggestions);
    }

    /** El proveedor no tiene mínimo o el pedido ya lo alcanza. */
    public static MoqCompletionResult nothingToAdd() {
        return new MoqCompletionResult(List.of(), false);
    }

    /** Lo que costarían todas las líneas sugeridas juntas. */
    public BigDecimal extraCost() {
        return suggestions.stream()
                .map(MoqSuggestion::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
