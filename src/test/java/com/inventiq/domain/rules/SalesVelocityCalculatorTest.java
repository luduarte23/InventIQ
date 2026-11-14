package com.inventiq.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Pruebas de la velocidad de ventas, sin Spring ni base de datos. */
class SalesVelocityCalculatorTest {

    @Test
    void itDividesTheUnitsSoldByTheWindowDays() {
        // 60 unidades en 30 días: 2 por día.
        Optional<BigDecimal> velocity = SalesVelocityCalculator.unitsPerDay(60, 30);

        assertEquals(new BigDecimal("2.0000"), velocity.get());
    }

    @Test
    void daysWithoutSalesAlsoCount() {
        // 15 unidades en 30 días: 0.5 por día, aunque se hayan vendido en pocos días.
        Optional<BigDecimal> velocity = SalesVelocityCalculator.unitsPerDay(15, 30);

        assertEquals(new BigDecimal("0.5000"), velocity.get());
    }

    @Test
    void theResultIsRoundedToFourDecimals() {
        assertEquals(new BigDecimal("3.3333"), SalesVelocityCalculator.unitsPerDay(10, 3).get());
        assertEquals(new BigDecimal("0.6667"), SalesVelocityCalculator.unitsPerDay(2, 3).get());
    }

    @Test
    void withoutSalesThereIsNoVelocity() {
        // Sin ventas el resultado es vacío, no cero.
        assertTrue(SalesVelocityCalculator.unitsPerDay(0, 30).isEmpty());
    }

    @Test
    void theWindowMustHaveAtLeastOneDay() {
        assertThrows(IllegalArgumentException.class,
                () -> SalesVelocityCalculator.unitsPerDay(10, 0));
    }

    @Test
    void theUnitsSoldCannotBeNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> SalesVelocityCalculator.unitsPerDay(-1, 30));
    }
}
