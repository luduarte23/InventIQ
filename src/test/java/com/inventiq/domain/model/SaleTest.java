package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas de Sale: ítems, total y cierre. Java puro, sin Spring ni base de datos. */
class SaleTest {

    private static final Instant OPENED = Instant.parse("2026-11-05T15:00:00Z");
    private static final Instant CLOSED = Instant.parse("2026-11-05T15:03:00Z");

    @Test
    void anOpenedSaleStartsEmptyAndOpen() {
        Sale sale = Sale.open(1L, OPENED);

        assertNull(sale.getId());
        assertEquals(1L, sale.getCashierId());
        assertEquals(OPENED, sale.getCreatedAt());
        assertNull(sale.getClosedAt());
        assertFalse(sale.isClosed());
        assertTrue(sale.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, sale.getTotal());
    }

    @Test
    void cashierAndCreatedAtAreRequired() {
        assertThrows(NullPointerException.class, () -> Sale.open(null, OPENED));
        assertThrows(NullPointerException.class, () -> Sale.open(1L, null));
    }

    @Test
    void nullItemsAreTreatedAsAnEmptyList() {
        Sale sale = new Sale(3L, 1L, OPENED, null, null);

        assertTrue(sale.getItems().isEmpty());
    }

    @Test
    void theTotalIsTheSumOfTheSubtotals() {
        Sale sale = Sale.open(1L, OPENED);

        sale.addItem(item(2, "3200.00"));   // 6400.00
        sale.addItem(item(1, "2720.00"));   // 2720.00 (lote con 15% de descuento)

        assertEquals(2, sale.getItems().size());
        assertEquals(new BigDecimal("9120.00"), sale.getTotal());
    }

    @Test
    void aSaleRebuiltWithItemsCalculatesItsTotal() {
        Sale sale = new Sale(3L, 1L, OPENED, CLOSED, List.of(item(2, "3200.00")));

        assertTrue(sale.isClosed());
        assertEquals(new BigDecimal("6400.00"), sale.getTotal());
    }

    @Test
    void aNullItemIsRejected() {
        Sale sale = Sale.open(1L, OPENED);

        assertThrows(NullPointerException.class, () -> sale.addItem(null));
    }

    @Test
    void closeMarksTheSaleAsClosed() {
        Sale sale = Sale.open(1L, OPENED);
        sale.addItem(item(1, "3200.00"));

        sale.close(CLOSED);

        assertTrue(sale.isClosed());
        assertEquals(CLOSED, sale.getClosedAt());
    }

    @Test
    void aSaleWithoutItemsCannotBeClosed() {
        Sale sale = Sale.open(1L, OPENED);

        assertThrows(IllegalStateException.class, () -> sale.close(CLOSED));
    }

    @Test
    void aClosedSaleRejectsNewItemsAndASecondClose() {
        Sale sale = Sale.open(1L, OPENED);
        sale.addItem(item(1, "3200.00"));
        sale.close(CLOSED);

        assertThrows(IllegalStateException.class, () -> sale.addItem(item(1, "3200.00")));
        assertThrows(IllegalStateException.class, () -> sale.close(CLOSED));
    }

    @Test
    void theItemListCannotBeModifiedFromOutside() {
        Sale sale = Sale.open(1L, OPENED);

        assertThrows(UnsupportedOperationException.class,
                () -> sale.getItems().add(item(1, "3200.00")));
    }

    @Test
    void salesWithTheSameIdAreEqual() {
        Sale a = new Sale(3L, 1L, OPENED, null, List.of());
        Sale b = new Sale(3L, 2L, CLOSED, null, List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void salesWithoutIdAreOnlyEqualToThemselves() {
        Sale a = Sale.open(1L, OPENED);
        Sale b = Sale.open(1L, OPENED);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    /** Ítem del producto 1, lote 8, con el precio ya cobrado (con descuento si lo hay). */
    private static SaleItem item(int quantity, String unitPrice) {
        return new SaleItem(null, null, 1L, 8L, quantity, new BigDecimal(unitPrice), null);
    }
}