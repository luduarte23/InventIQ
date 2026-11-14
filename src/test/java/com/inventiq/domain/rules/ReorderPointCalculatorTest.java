package com.inventiq.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Pruebas del punto de reorden, sin Spring ni base de datos. */
class ReorderPointCalculatorTest {

    @Test
    void itAddsTheUnitsSoldDuringTheLeadTimeAndTheSafetyStock() {
        // 2 por día, el proveedor tarda 3 días y el stock de seguridad es 4: 6 + 4 = 10.
        int rop = ReorderPointCalculator.reorderPoint(velocity("2.0000"), 3, 4);

        assertEquals(10, rop);
    }

    @Test
    void theUnitsSoldAreRoundedUp() {
        // 0.5 por día durante 3 días son 1.5 unidades: se cubren 2.
        int rop = ReorderPointCalculator.reorderPoint(velocity("0.5000"), 3, 4);

        assertEquals(6, rop);
    }

    @Test
    void theRoundingOfTheVelocityDoesNotAddAnExtraUnit() {
        // 2 unidades en 3 días dan 0.6667 por día; en 3 días se venden 2, no 3.
        Optional<BigDecimal> twoInThreeDays = SalesVelocityCalculator.unitsPerDay(2, 3);

        assertEquals(2, ReorderPointCalculator.reorderPoint(twoInThreeDays, 3, 0));
    }

    @Test
    void withoutSalesHistoryTheReorderPointIsTheSafetyStock() {
        int rop = ReorderPointCalculator.reorderPoint(Optional.empty(), 3, 4);

        assertEquals(4, rop);
    }

    @Test
    void ifTheSupplierDeliversTheSameDayOnlyTheSafetyStockCounts() {
        int rop = ReorderPointCalculator.reorderPoint(velocity("5.0000"), 0, 4);

        assertEquals(4, rop);
    }

    @Test
    void negativeDaysOrSafetyStockAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ReorderPointCalculator.reorderPoint(velocity("2.0000"), -1, 4));
        assertThrows(IllegalArgumentException.class,
                () -> ReorderPointCalculator.reorderPoint(velocity("2.0000"), 3, -1));
    }

    @Test
    void theSuggestedQuantityCoversTheLeadTimeAndTheReviewPeriod() {
        // 2 por día durante 3 + 7 días = 20, más 4 de seguridad, menos 8 que hay: 16.
        int quantity = ReorderPointCalculator.suggestedQuantity(velocity("2.0000"), 3, 7, 4, 8);

        assertEquals(16, quantity);
    }

    @Test
    void theSuggestedQuantityRoundsTheUnitsUp() {
        // 0.25 por día durante 10 días son 2.5 unidades: se piden 3.
        int quantity = ReorderPointCalculator.suggestedQuantity(velocity("0.2500"), 3, 7, 0, 0);

        assertEquals(3, quantity);
    }

    @Test
    void withoutSalesHistoryItOrdersWhatIsMissingForTheSafetyStock() {
        // Sin historial: stock de seguridad 4 y queda 1, se piden 3.
        int quantity = ReorderPointCalculator.suggestedQuantity(Optional.empty(), 3, 7, 4, 1);

        assertEquals(3, quantity);
    }

    @Test
    void theSuggestedQuantityIsAtLeastOne() {
        int quantity = ReorderPointCalculator.suggestedQuantity(velocity("2.0000"), 3, 7, 4, 100);

        assertEquals(1, quantity);
    }

    @Test
    void negativeValuesAreRejectedInTheSuggestedQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> ReorderPointCalculator.suggestedQuantity(velocity("2.0000"), 3, -1, 4, 8));
        assertThrows(IllegalArgumentException.class,
                () -> ReorderPointCalculator.suggestedQuantity(velocity("2.0000"), 3, 7, 4, -1));
    }

    private static Optional<BigDecimal> velocity(String unitsPerDay) {
        return Optional.of(new BigDecimal(unitsPerDay));
    }
}
