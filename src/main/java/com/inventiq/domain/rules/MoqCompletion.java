package com.inventiq.domain.rules;

import com.inventiq.domain.model.Supplier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pedido mínimo del proveedor (MOQ). Si un borrador no llega al mínimo en dinero o en
 * unidades que pide el proveedor para dar envío gratis o descuento, sugiere agregar
 * productos suyos que se venden rápido, hasta alcanzarlo.
 *
 * Solo sugiere: el administrador acepta o modifica las líneas. Si el presupuesto no alcanza
 * para llegar al mínimo, no sugiere nada y lo avisa.
 */
public class MoqCompletion {

    /**
     * Calcula qué agregar al pedido. Recorre los candidatos del que más se vende al que
     * menos, y de cada uno toma como máximo lo que vende en un periodo de revisión, para
     * no llenar la bodega con un solo producto.
     *
     * @param orderTotal      costo actual del borrador
     * @param orderUnits      unidades actuales del borrador
     * @param candidates      productos del proveedor que no están en el pedido
     * @param availableBudget presupuesto disponible para este pedido
     */
    public MoqCompletionResult complete(Supplier supplier, BigDecimal orderTotal, int orderUnits,
                                        List<MoqCandidate> candidates, int reviewPeriodDays,
                                        BigDecimal availableBudget) {
        Objects.requireNonNull(supplier, "supplier es obligatorio");
        Objects.requireNonNull(orderTotal, "orderTotal es obligatorio");
        Objects.requireNonNull(availableBudget, "availableBudget es obligatorio");

        BigDecimal missingAmount = missingAmount(supplier, orderTotal);
        int missingUnits = missingUnits(supplier, orderUnits);
        if (missingAmount.signum() <= 0 && missingUnits <= 0) {
            return MoqCompletionResult.nothingToAdd();
        }

        // Primero los que más se venden; a igual velocidad, por id para que el orden no cambie.
        List<MoqCandidate> byRotation = new ArrayList<>(candidates);
        byRotation.sort(Comparator.comparing(MoqCandidate::unitsPerDay).reversed()
                .thenComparing(MoqCandidate::productId));

        List<MoqSuggestion> suggestions = new ArrayList<>();
        for (MoqCandidate candidate : byRotation) {
            if (missingAmount.signum() <= 0 && missingUnits <= 0) {
                break;
            }
            int maxUnits = ReorderPointCalculator.unitsSoldIn(candidate.unitsPerDay(), reviewPeriodDays);
            int quantity = Math.min(maxUnits, unitsNeeded(candidate, missingAmount, missingUnits));
            if (quantity <= 0) {
                continue;
            }
            BigDecimal lineCost = candidate.unitCost().multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            suggestions.add(new MoqSuggestion(candidate.productId(), quantity, lineCost));
            missingAmount = missingAmount.subtract(lineCost);
            missingUnits -= quantity;
        }

        MoqCompletionResult result = new MoqCompletionResult(suggestions, false);
        if (orderTotal.add(result.extraCost()).compareTo(availableBudget) > 0) {
            return new MoqCompletionResult(List.of(), true);
        }
        return result;
    }

    /** Dinero que falta para el mínimo; cero si el proveedor no pide mínimo en dinero. */
    private static BigDecimal missingAmount(Supplier supplier, BigDecimal orderTotal) {
        if (supplier.getMinOrderAmount() == null) {
            return BigDecimal.ZERO;
        }
        return supplier.getMinOrderAmount().subtract(orderTotal);
    }

    /** Unidades que faltan para el mínimo; cero si el proveedor no pide mínimo en unidades. */
    private static int missingUnits(Supplier supplier, int orderUnits) {
        if (supplier.getMinOrderUnits() == null) {
            return 0;
        }
        return supplier.getMinOrderUnits() - orderUnits;
    }

    /** Unidades de este candidato que cubren lo que falta, en dinero y en unidades. */
    private static int unitsNeeded(MoqCandidate candidate, BigDecimal missingAmount,
                                   int missingUnits) {
        int forAmount = 0;
        if (missingAmount.signum() > 0 && candidate.unitCost().signum() > 0) {
            forAmount = missingAmount.divide(candidate.unitCost(), 0, RoundingMode.CEILING)
                    .intValueExact();
        }
        return Math.max(forAmount, Math.max(0, missingUnits));
    }
}
