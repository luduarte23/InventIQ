package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** Pruebas de SaleItem y de la fórmula de precio con descuento de ACUERDOS §3. */
class SaleItemTest {

    private static final BigDecimal PRICE = new BigDecimal("3200.00");

    // --- Constructor y validaciones ---

    @Test
    void aValidItemKeepsItsData() {
        SaleItem item = new SaleItem(4L, 2L, 1L, 8L, 3, PRICE, new BigDecimal("15.00"));

        assertEquals(4L, item.getId());
        assertEquals(2L, item.getSaleId());
        assertEquals(1L, item.getProductId());
        assertEquals(8L, item.getBatchId());
        assertEquals(3, item.getQuantity());
        assertEquals(PRICE, item.getUnitPrice());
        assertEquals(new BigDecimal("15.00"), item.getDiscountPercent());
    }

    @Test
    void withoutDiscountTheDiscountIsZero() {
        SaleItem item = new SaleItem(null, null, 1L, 8L, 1, PRICE, null);

        assertEquals(BigDecimal.ZERO, item.getDiscountPercent());
    }

    @Test
    void productAndBatchAreRequired() {
        assertThrows(NullPointerException.class,
                () -> new SaleItem(null, null, null, 8L, 1, PRICE, null));
        assertThrows(NullPointerException.class,
                () -> new SaleItem(null, null, 1L, null, 1, PRICE, null));
    }

    @Test
    void quantityMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new SaleItem(null, null, 1L, 8L, 0, PRICE, null));
    }

    @Test
    void unitPriceIsRequiredAndCannotBeNegative() {
        assertThrows(NullPointerException.class,
                () -> new SaleItem(null, null, 1L, 8L, 1, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SaleItem(null, null, 1L, 8L, 1, new BigDecimal("-1"), null));
    }

    @Test
    void discountMustBeBetweenZeroAndOneHundred() {
        assertThrows(IllegalArgumentException.class,
                () -> new SaleItem(null, null, 1L, 8L, 1, PRICE, new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class,
                () -> new SaleItem(null, null, 1L, 8L, 1, PRICE, new BigDecimal("100.01")));
    }

    // --- Cálculos ---

    @Test
    void subtotalIsQuantityTimesUnitPriceWithoutDiscountingAgain() {
        // unitPrice ya trae el descuento (§3): el subtotal NO vuelve a descontar.
        SaleItem item = new SaleItem(null, null, 1L, 8L, 2, new BigDecimal("2720.00"),
                new BigDecimal("15.00"));

        assertEquals(new BigDecimal("5440.00"), item.subtotal());
    }

    @Test
    void priceAfterDiscountAppliesTheFormulaOfSection3() {
        // 3200 * (1 - 15/100) = 2720.00
        assertEquals(new BigDecimal("2720.00"),
                SaleItem.priceAfterDiscount(PRICE, new BigDecimal("15.00")));
        assertEquals(new BigDecimal("3200.00"),
                SaleItem.priceAfterDiscount(PRICE, BigDecimal.ZERO));
        assertEquals(new BigDecimal("0.00"),
                SaleItem.priceAfterDiscount(PRICE, new BigDecimal("100")));
    }

    @Test
    void priceAfterDiscountRoundsHalfUpToTwoDecimals() {
        // 1000.05 * 0.5 = 500.025 -> HALF_UP -> 500.03
        assertEquals(new BigDecimal("500.03"),
                SaleItem.priceAfterDiscount(new BigDecimal("1000.05"), new BigDecimal("50")));
    }

    @Test
    void priceAfterDiscountRejectsInvalidPercentages() {
        assertThrows(NullPointerException.class, () -> SaleItem.priceAfterDiscount(PRICE, null));
        assertThrows(IllegalArgumentException.class,
                () -> SaleItem.priceAfterDiscount(PRICE, new BigDecimal("101")));
    }

    // --- Igualdad por id ---

    @Test
    void itemsWithTheSameIdAreEqual() {
        SaleItem a = new SaleItem(4L, null, 1L, 8L, 1, PRICE, null);
        SaleItem b = new SaleItem(4L, null, 2L, 9L, 5, PRICE, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void itemsWithoutIdAreOnlyEqualToThemselves() {
        SaleItem a = new SaleItem(null, null, 1L, 8L, 1, PRICE, null);
        SaleItem b = new SaleItem(null, null, 1L, 8L, 1, PRICE, null);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }
}
