package com.inventiq.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Supplier;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas del pedido mínimo, sin Spring ni base de datos. */
class MoqCompletionTest {

    private static final int REVIEW_DAYS = 7;

    private final MoqCompletion moq = new MoqCompletion();

    @Test
    void itSuggestsProductsUntilTheMinimumAmountIsReached() {
        // El proveedor pide 500.000 y el borrador va en 400.000: faltan 100.000.
        Supplier supplier = supplierWithMinimumAmount("500000");
        List<MoqCandidate> candidates = List.of(
                candidate(1L, "3.0000", "10000"),
                candidate(2L, "1.0000", "20000"));

        MoqCompletionResult result = moq.complete(supplier, money("400000"), 40, candidates,
                REVIEW_DAYS, money("600000"));

        // 10 unidades del que más se vende cubren los 100.000.
        assertEquals(1, result.suggestions().size());
        assertEquals(new MoqSuggestion(1L, 10, money("100000.00")), result.suggestions().get(0));
        assertFalse(result.blockedByBudget());
    }

    @Test
    void eachProductAddsAtMostWhatItSellsInAReviewPeriod() {
        // Faltan 100.000. El producto 2 se vende más, pero en 7 días solo vende 14.
        Supplier supplier = supplierWithMinimumAmount("500000");
        List<MoqCandidate> candidates = List.of(
                candidate(1L, "0.5000", "10000"),
                candidate(2L, "2.0000", "5000"));

        MoqCompletionResult result = moq.complete(supplier, money("400000"), 40, candidates,
                REVIEW_DAYS, money("600000"));

        // 14 del producto 2 (70.000) y 3 del producto 1 (30.000).
        assertEquals(List.of(
                new MoqSuggestion(2L, 14, money("70000.00")),
                new MoqSuggestion(1L, 3, money("30000.00"))), result.suggestions());
    }

    @Test
    void withoutBudgetToReachTheMinimumNothingIsAdded() {
        // Presupuesto de 400.000 y el borrador ya cuesta 400.000.
        Supplier supplier = supplierWithMinimumAmount("500000");
        List<MoqCandidate> candidates = List.of(candidate(1L, "3.0000", "10000"));

        MoqCompletionResult result = moq.complete(supplier, money("400000"), 40, candidates,
                REVIEW_DAYS, money("400000"));

        assertTrue(result.suggestions().isEmpty());
        assertTrue(result.blockedByBudget());
    }

    @Test
    void itAlsoWorksWithAMinimumInUnits() {
        // El proveedor pide 50 unidades y el borrador lleva 40.
        Supplier supplier = new Supplier(1L, "Lácteos", "+573001234567", 3, null, 50, true);
        List<MoqCandidate> candidates = List.of(candidate(1L, "2.0000", "1000"));

        MoqCompletionResult result = moq.complete(supplier, money("40000"), 40, candidates,
                REVIEW_DAYS, money("600000"));

        assertEquals(new MoqSuggestion(1L, 10, money("10000.00")), result.suggestions().get(0));
    }

    @Test
    void ifTheSupplierHasNoMinimumOrItIsReachedNothingIsSuggested() {
        Supplier withoutMinimum = new Supplier(1L, "Aseo", "+573001234567", 3, null, null, true);
        List<MoqCandidate> candidates = List.of(candidate(1L, "3.0000", "10000"));

        MoqCompletionResult noMinimum = moq.complete(withoutMinimum, money("100000"), 10,
                candidates, REVIEW_DAYS, money("600000"));
        MoqCompletionResult reached = moq.complete(supplierWithMinimumAmount("500000"),
                money("520000"), 50, candidates, REVIEW_DAYS, money("600000"));

        assertEquals(MoqCompletionResult.nothingToAdd(), noMinimum);
        assertEquals(MoqCompletionResult.nothingToAdd(), reached);
    }

    @Test
    void theExtraCostIsTheSumOfTheSuggestedLines() {
        MoqCompletionResult result = new MoqCompletionResult(List.of(
                new MoqSuggestion(1L, 2, money("3000.00")),
                new MoqSuggestion(2L, 1, money("1500.00"))), false);

        assertEquals(money("4500.00"), result.extraCost());
    }

    private static Supplier supplierWithMinimumAmount(String amount) {
        return new Supplier(1L, "Lácteos", "+573001234567", 3, money(amount), null, true);
    }

    private static MoqCandidate candidate(Long productId, String unitsPerDay, String unitCost) {
        return new MoqCandidate(productId, new BigDecimal(unitsPerDay), money(unitCost));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
