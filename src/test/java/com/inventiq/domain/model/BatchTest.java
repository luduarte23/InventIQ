package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de Batch (lote): consumo, devolución, vencimiento y descuento. */
class BatchTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 5);
    private static final Instant RECEIVED = Instant.parse("2026-11-01T12:00:00Z");
    private static final BigDecimal COST = new BigDecimal("2100.00");

    // --- Constructor y validaciones ---

    @Test
    void aValidBatchKeepsItsData() {
        Batch batch = new Batch(8L, 1L, 10, 6, TODAY.plusDays(3), RECEIVED, COST,
                new BigDecimal("10.00"), 4L, 2L);

        assertEquals(8L, batch.getId());
        assertEquals(1L, batch.getProductId());
        assertEquals(10, batch.getQuantityReceived());
        assertEquals(6, batch.getQuantityRemaining());
        assertEquals(TODAY.plusDays(3), batch.getExpirationDate());
        assertEquals(RECEIVED, batch.getReceivedAt());
        assertEquals(COST, batch.getUnitCost());
        assertEquals(new BigDecimal("10.00"), batch.getDiscountPercent());
        assertEquals(4L, batch.getPurchaseOrderId());
        assertEquals(2L, batch.getVersion());
    }

    @Test
    void withoutDiscountTheDiscountIsZero() {
        Batch batch = new Batch(null, 1L, 10, 10, null, RECEIVED, COST, null, null, null);

        assertEquals(BigDecimal.ZERO, batch.getDiscountPercent());
        assertNull(batch.getPurchaseOrderId());
    }

    @Test
    void productReceivedAtAndUnitCostAreRequired() {
        assertThrows(NullPointerException.class,
                () -> new Batch(null, null, 10, 10, null, RECEIVED, COST, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new Batch(null, 1L, 10, 10, null, null, COST, null, null, null));
        assertThrows(NullPointerException.class,
                () -> new Batch(null, 1L, 10, 10, null, RECEIVED, null, null, null, null));
    }

    @Test
    void quantitiesMustBeConsistent() {
        // recibido negativo
        assertThrows(IllegalArgumentException.class,
                () -> new Batch(null, 1L, -1, 0, null, RECEIVED, COST, null, null, null));
        // restante negativo
        assertThrows(IllegalArgumentException.class,
                () -> new Batch(null, 1L, 10, -1, null, RECEIVED, COST, null, null, null));
        // restante mayor que lo recibido
        assertThrows(IllegalArgumentException.class,
                () -> new Batch(null, 1L, 10, 11, null, RECEIVED, COST, null, null, null));
    }

    @Test
    void unitCostCannotBeNegative() {
        assertThrows(IllegalArgumentException.class, () -> new Batch(null, 1L, 10, 10, null,
                RECEIVED, new BigDecimal("-1"), null, null, null));
    }

    @Test
    void discountMustBeBetweenZeroAndOneHundred() {
        assertThrows(IllegalArgumentException.class, () -> new Batch(null, 1L, 10, 10, null,
                RECEIVED, COST, new BigDecimal("-5"), null, null));
        assertThrows(IllegalArgumentException.class, () -> new Batch(null, 1L, 10, 10, null,
                RECEIVED, COST, new BigDecimal("150"), null, null));
    }

    // --- consume y restore ---

    @Test
    void consumeSubtractsUnitsUntilTheBatchIsDepleted() {
        Batch batch = batch(5);

        batch.consume(3);
        assertEquals(2, batch.getQuantityRemaining());
        assertFalse(batch.isDepleted());

        batch.consume(2);
        assertEquals(0, batch.getQuantityRemaining());
        assertTrue(batch.isDepleted());
    }

    @Test
    void consumeRejectsZeroAndMoreThanRemaining() {
        Batch batch = batch(5);

        assertThrows(IllegalArgumentException.class, () -> batch.consume(0));
        assertThrows(IllegalStateException.class, () -> batch.consume(6));
        assertEquals(5, batch.getQuantityRemaining());
    }

    @Test
    void restoreGivesUnitsBack() {
        Batch batch = batch(5);
        batch.consume(4);

        batch.restore(4);

        assertEquals(5, batch.getQuantityRemaining());
    }

    @Test
    void restoreRejectsZeroAndMoreThanWasReceived() {
        Batch batch = batch(5);
        batch.consume(2);

        assertThrows(IllegalArgumentException.class, () -> batch.restore(0));
        assertThrows(IllegalStateException.class, () -> batch.restore(3));
        assertEquals(3, batch.getQuantityRemaining());
    }

    // --- Vencimiento (§5.2: vence si expirationDate < hoy) ---

    @Test
    void aBatchExpiresTheDayAfterItsExpirationDate() {
        Batch expiresToday = batchExpiring(TODAY);
        Batch expiredYesterday = batchExpiring(TODAY.minusDays(1));

        assertFalse(expiresToday.isExpired(TODAY));
        assertTrue(expiredYesterday.isExpired(TODAY));
        assertTrue(expiresToday.hasExpirationDate());
    }

    @Test
    void aBatchWithoutExpirationDateNeverExpires() {
        Batch batch = batchExpiring(null);

        assertFalse(batch.hasExpirationDate());
        assertFalse(batch.isExpired(TODAY.plusYears(10)));
    }

    // --- Descuento ---

    @Test
    void applyDiscountChangesThePercentage() {
        Batch batch = batch(5);

        batch.applyDiscount(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("30.00"), batch.getDiscountPercent());
    }

    @Test
    void applyDiscountRejectsInvalidPercentages() {
        Batch batch = batch(5);

        assertThrows(NullPointerException.class, () -> batch.applyDiscount(null));
        assertThrows(IllegalArgumentException.class,
                () -> batch.applyDiscount(new BigDecimal("100.01")));
    }

    // --- Igualdad por id ---

    @Test
    void batchesWithTheSameIdAreEqual() {
        Batch a = new Batch(8L, 1L, 10, 10, null, RECEIVED, COST, null, null, 0L);
        Batch b = new Batch(8L, 2L, 3, 1, null, RECEIVED, COST, null, null, 5L);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void batchesWithoutIdAreOnlyEqualToThemselves() {
        Batch a = batch(5);
        Batch b = batch(5);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    /** Lote nuevo (sin id) del producto 1, sin vencimiento, con todas sus unidades. */
    private static Batch batch(int quantity) {
        return new Batch(null, 1L, quantity, quantity, null, RECEIVED, COST, null, null, null);
    }

    private static Batch batchExpiring(LocalDate expirationDate) {
        return new Batch(null, 1L, 5, 5, expirationDate, RECEIVED, COST, null, null, null);
    }
}
