package com.inventiq.domain.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.exception.InsufficientStockException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas de la regla FIFO (B2): Java puro, sin Spring ni base de datos. */
class FifoAllocatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);

    private final FifoAllocator fifo = new FifoAllocator();

    @Test
    void us03Scenario1_twoUnitsComeFromTheBatchThatExpiresFirst() {
        // Yogur con 6 unidades: 3 vencen el 08/10/2026 y 3 el 30/10/2026.
        Batch oct30 = batch(1L, 3, LocalDate.of(2026, 10, 30));
        Batch oct8 = batch(2L, 3, LocalDate.of(2026, 10, 8));

        List<BatchAllocation> result = fifo.allocate(1L, List.of(oct30, oct8), 2, TODAY);

        assertEquals(1, result.size());
        assertEquals(oct8, result.get(0).batch());
        assertEquals(2, result.get(0).quantity());
    }

    @Test
    void itContinuesWithTheNextBatchAndBatchesWithoutExpirationGoLast() {
        Batch noExpiration = batch(1L, 5, null);
        Batch oct30 = batch(2L, 3, LocalDate.of(2026, 10, 30));
        Batch oct8 = batch(3L, 3, LocalDate.of(2026, 10, 8));

        List<BatchAllocation> result = fifo.allocate(1L, List.of(noExpiration, oct30, oct8), 7, TODAY);

        // Una línea por lote: 3 del 08/10, 3 del 30/10 y 1 del que no vence.
        assertEquals(3, result.size());
        assertEquals(oct8, result.get(0).batch());
        assertEquals(oct30, result.get(1).batch());
        assertEquals(noExpiration, result.get(2).batch());
        assertEquals(1, result.get(2).quantity());
    }

    @Test
    void expiredBatchesAreNotSold() {
        Batch expired = batch(1L, 10, TODAY.minusDays(1));
        Batch valid = batch(2L, 4, TODAY.plusDays(10));

        List<BatchAllocation> result = fifo.allocate(1L, List.of(expired, valid), 2, TODAY);

        assertEquals(valid, result.get(0).batch());
    }

    @Test
    void ifTheSellableUnitsAreNotEnoughItFails() {
        Batch expired = batch(1L, 10, TODAY.minusDays(1));
        Batch valid = batch(2L, 3, TODAY.plusDays(10));

        assertThrows(InsufficientStockException.class,
                () -> fifo.allocate(1L, List.of(expired, valid), 5, TODAY));
    }

    /** Lote de prueba del producto 1, recibido el 01/10/2026. */
    private static Batch batch(Long id, int quantity, LocalDate expiration) {
        return new Batch(id, 1L, quantity, quantity, expiration, Instant.parse("2026-10-01T12:00:00Z"),
                new BigDecimal("1000"), BigDecimal.ZERO, null, 0L);
    }
}
