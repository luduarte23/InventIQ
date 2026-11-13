package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de PurchaseOrderLine: Java puro, sin Spring ni base de datos.
 * Está en el mismo paquete que la clase, así que también puede probar los métodos
 * package-private (edit y registerReceived) que en producción solo usa PurchaseOrder.
 */
class PurchaseOrderLineTest {

    private static final BigDecimal COST = new BigDecimal("2100.50");

    // --- Constructor y validaciones ---

    @Test
    void aValidLineKeepsItsData() {
        PurchaseOrderLine line = new PurchaseOrderLine(7L, 3L, 1L, 10, 4, COST, true);

        assertEquals(7L, line.getId());
        assertEquals(3L, line.getOrderId());
        assertEquals(1L, line.getProductId());
        assertEquals(10, line.getQuantityOrdered());
        assertEquals(4, line.getQuantityReceived());
        assertEquals(COST, line.getUnitCost());
        assertTrue(line.isAddedByMoq());
    }

    @Test
    void productIdIsRequired() {
        assertThrows(NullPointerException.class,
                () -> new PurchaseOrderLine(null, null, null, 10, 0, COST, false));
    }

    @Test
    void quantityOrderedMustBeGreaterThanZero() {
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseOrderLine(null, null, 1L, 0, 0, COST, false));
    }

    @Test
    void quantityReceivedCannotBeNegativeNorExceedTheOrderedQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseOrderLine(null, null, 1L, 10, -1, COST, false));
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseOrderLine(null, null, 1L, 10, 11, COST, false));
    }

    @Test
    void unitCostIsRequiredAndCannotBeNegative() {
        assertThrows(NullPointerException.class,
                () -> new PurchaseOrderLine(null, null, 1L, 10, 0, null, false));
        assertThrows(IllegalArgumentException.class,
                () -> new PurchaseOrderLine(null, null, 1L, 10, 0, new BigDecimal("-1"), false));
    }

    // --- Cálculos ---

    @Test
    void lineCostIsUnitCostTimesQuantityWithTwoDecimals() {
        PurchaseOrderLine line = newLine(3);

        // 2100.50 * 3 = 6301.50
        assertEquals(new BigDecimal("6301.50"), line.lineCost());
    }

    @Test
    void pendingQuantityAndFullyReceived() {
        PurchaseOrderLine partial = new PurchaseOrderLine(null, null, 1L, 10, 4, COST, false);
        PurchaseOrderLine complete = new PurchaseOrderLine(null, null, 1L, 10, 10, COST, false);

        assertEquals(6, partial.pendingQuantity());
        assertFalse(partial.isFullyReceived());
        assertEquals(0, complete.pendingQuantity());
        assertTrue(complete.isFullyReceived());
    }

    // --- Cambios (package-private) ---

    @Test
    void editChangesQuantityAndMoqFlag() {
        PurchaseOrderLine line = newLine(10);

        line.edit(15, true);

        assertEquals(15, line.getQuantityOrdered());
        assertTrue(line.isAddedByMoq());
    }

    @Test
    void editRejectsQuantityZeroOrLess() {
        PurchaseOrderLine line = newLine(10);

        assertThrows(IllegalArgumentException.class, () -> line.edit(0, false));
    }

    @Test
    void registerReceivedAddsUnits() {
        PurchaseOrderLine line = newLine(10);

        line.registerReceived(4);
        line.registerReceived(6);

        assertEquals(10, line.getQuantityReceived());
        assertTrue(line.isFullyReceived());
    }

    @Test
    void registerReceivedRejectsZeroAndMoreThanPending() {
        PurchaseOrderLine line = newLine(10);
        line.registerReceived(8);

        assertThrows(IllegalArgumentException.class, () -> line.registerReceived(0));
        assertThrows(IllegalArgumentException.class, () -> line.registerReceived(3));
        assertEquals(8, line.getQuantityReceived());
    }

    // --- Igualdad por id ---

    @Test
    void linesWithTheSameIdAreEqual() {
        PurchaseOrderLine a = new PurchaseOrderLine(5L, null, 1L, 10, 0, COST, false);
        PurchaseOrderLine b = new PurchaseOrderLine(5L, null, 2L, 3, 0, COST, false);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void linesWithoutIdAreOnlyEqualToThemselves() {
        PurchaseOrderLine a = newLine(10);
        PurchaseOrderLine b = newLine(10);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    /** Línea nueva (sin id) del producto 1, sin unidades recibidas. */
    private static PurchaseOrderLine newLine(int quantityOrdered) {
        return new PurchaseOrderLine(null, null, 1L, quantityOrdered, 0, COST, false);
    }
}
