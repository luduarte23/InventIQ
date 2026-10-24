package co.edu.unal.inventiq.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.InsufficientStockException;
import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;
import co.edu.unal.inventiq.domain.model.Batch;

class FifoAllocatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 9, 20, 8, 0);

    private final FifoAllocator fifo = new FifoAllocator();

    @Test
    void takesFromTheBatchThatExpiresFirst_US03Scenario1() {
        Batch expiresOct8 = batch(1, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        Batch expiresOct30 = batch(2, 3, LocalDate.of(2026, 10, 30), RECEIVED);

        List<BatchAllocation> allocations = fifo.allocate("Yogur", List.of(expiresOct30, expiresOct8), 2, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(1, 2));
    }

    @Test
    void spreadsTheQuantityOverSeveralBatchesWithOneAllocationEach() {
        Batch first = batch(1, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        Batch second = batch(2, 3, LocalDate.of(2026, 10, 30), RECEIVED);

        List<BatchAllocation> allocations = fifo.allocate("Yogur", List.of(first, second), 5, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(1, 3), new BatchAllocation(2, 2));
    }

    @Test
    void sameExpirationDateTakesTheBatchReceivedFirst() {
        LocalDate sameDay = LocalDate.of(2026, 10, 20);
        Batch receivedLater = batch(1, 5, sameDay, RECEIVED.plusDays(3));
        Batch receivedEarlier = batch(2, 5, sameDay, RECEIVED);

        List<BatchAllocation> allocations = fifo.allocate("Leche", List.of(receivedLater, receivedEarlier), 1, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(2, 1));
    }

    @Test
    void batchesWithoutExpirationAreSoldAfterPerishableOnes() {
        Batch noExpiration = batch(1, 5, null, RECEIVED.minusDays(30));
        Batch perishable = batch(2, 5, LocalDate.of(2026, 12, 1), RECEIVED);

        List<BatchAllocation> allocations = fifo.allocate("Arroz", List.of(noExpiration, perishable), 6, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(2, 5), new BatchAllocation(1, 1));
    }

    @Test
    void batchesWithoutExpirationAreSoldInReceptionOrder() {
        Batch newer = batch(1, 5, null, RECEIVED);
        Batch older = batch(2, 5, null, RECEIVED.minusDays(10));

        List<BatchAllocation> allocations = fifo.allocate("Arroz", List.of(newer, older), 2, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(2, 2));
    }

    @Test
    void expiredBatchesAreNeverSold() {
        Batch expired = batch(1, 10, TODAY.minusDays(1), RECEIVED);
        Batch valid = batch(2, 4, TODAY.plusDays(5), RECEIVED);

        List<BatchAllocation> allocations = fifo.allocate("Leche", List.of(expired, valid), 3, TODAY);

        assertThat(allocations).containsExactly(new BatchAllocation(2, 3));
    }

    @Test
    void aBatchThatExpiresTodayCanStillBeSold() {
        Batch expiresToday = batch(1, 2, TODAY, RECEIVED);

        assertThat(fifo.allocate("Pan", List.of(expiresToday), 2, TODAY))
                .containsExactly(new BatchAllocation(1, 2));
    }

    @Test
    void emptyBatchesAreSkipped() {
        Batch empty = new Batch(1L, 1, 5, 0, LocalDate.of(2026, 10, 6), RECEIVED, 0);
        Batch next = batch(2, 5, LocalDate.of(2026, 10, 9), RECEIVED);

        assertThat(fifo.allocate("Kumis", List.of(empty, next), 1, TODAY))
                .containsExactly(new BatchAllocation(2, 1));
    }

    @Test
    void insufficientStockFailsWithoutPartialAllocationAndIgnoresExpiredUnits() {
        Batch expired = batch(1, 10, TODAY.minusDays(2), RECEIVED);
        Batch valid = batch(2, 3, TODAY.plusDays(2), RECEIVED);

        assertThatThrownBy(() -> fifo.allocate("Queso", List.of(expired, valid), 4, TODAY))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getRequested()).isEqualTo(4);
                    assertThat(e.getAvailable()).isEqualTo(3);
                    assertThat(e.getMessage()).contains("Queso");
                });
    }

    @Test
    void noBatchesMeansNoStock() {
        assertThatThrownBy(() -> fifo.allocate("Kumis", List.of(), 1, TODAY))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void quantityMustBePositive() {
        Batch batch = batch(1, 3, null, RECEIVED);

        assertThatThrownBy(() -> fifo.allocate("Arroz", List.of(batch), 0, TODAY))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void sellableQuantityCountsOnlyNonExpiredUnits() {
        List<Batch> batches = List.of(
                batch(1, 4, TODAY.minusDays(1), RECEIVED),
                batch(2, 3, TODAY.plusDays(1), RECEIVED),
                batch(3, 5, null, RECEIVED));

        assertThat(fifo.sellableQuantity(batches, TODAY)).isEqualTo(8);
    }

    @Test
    void fifoOrderSortsByExpirationThenReceptionThenId() {
        Batch c = batch(3, 1, null, RECEIVED);
        Batch b = batch(2, 1, LocalDate.of(2026, 11, 1), RECEIVED);
        Batch a = batch(1, 1, LocalDate.of(2026, 10, 10), RECEIVED);

        assertThat(List.of(c, b, a).stream().sorted(FifoAllocator.FIFO_ORDER).map(Batch::getId))
                .containsExactly(1L, 2L, 3L);
    }

    private static Batch batch(long id, int quantity, LocalDate expiration, LocalDateTime receivedAt) {
        return new Batch(id, 1, quantity, quantity, expiration, receivedAt, 0);
    }
}
