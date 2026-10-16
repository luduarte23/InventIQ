package co.edu.unal.inventiq.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;

class BatchTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Test
    void deductReducesTheAvailableUnits() {
        Batch batch = new Batch(1L, 7, 10, 6, TODAY.plusDays(3), RECEIVED, 2);

        batch.deduct(4);

        assertThat(batch.getQuantityAvailable()).isEqualTo(2);
        assertThat(batch.getQuantityReceived()).isEqualTo(10);
        assertThat(batch.getProductId()).isEqualTo(7);
        assertThat(batch.getVersion()).isEqualTo(2);
    }

    @Test
    void cannotDeductMoreThanAvailable() {
        Batch batch = new Batch(1L, 1, 5, 2, null, RECEIVED, 0);

        assertThatThrownBy(() -> batch.deduct(3)).isInstanceOf(IllegalStateException.class);
        assertThat(batch.getQuantityAvailable()).isEqualTo(2);
    }

    @Test
    void cannotDeductZeroOrNegative() {
        Batch batch = new Batch(1L, 1, 5, 5, null, RECEIVED, 0);

        assertThatThrownBy(() -> batch.deduct(0)).isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void availableMustBeBetweenZeroAndReceived() {
        assertThatThrownBy(() -> new Batch(1L, 1, 5, 6, null, RECEIVED, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Batch(1L, 1, 5, -1, null, RECEIVED, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiresTheDayAfterItsExpirationDate() {
        Batch batch = new Batch(1L, 1, 5, 5, TODAY, RECEIVED, 0);

        assertThat(batch.isExpired(TODAY)).isFalse();
        assertThat(batch.isExpired(TODAY.plusDays(1))).isTrue();
        assertThat(batch.isSellable(TODAY)).isTrue();
        assertThat(batch.isSellable(TODAY.plusDays(1))).isFalse();
    }

    @Test
    void batchWithoutExpirationNeverExpires() {
        Batch batch = new Batch(1L, 1, 5, 5, null, RECEIVED, 0);

        assertThat(batch.isExpired(TODAY.plusYears(10))).isFalse();
        assertThat(batch.getExpirationDate()).isNull();
        assertThat(batch.getReceivedAt()).isEqualTo(RECEIVED);
    }

    @Test
    void emptyBatchIsNotSellable() {
        Batch batch = new Batch(1L, 1, 5, 0, TODAY.plusDays(1), RECEIVED, 0);

        assertThat(batch.isSellable(TODAY)).isFalse();
    }
}
