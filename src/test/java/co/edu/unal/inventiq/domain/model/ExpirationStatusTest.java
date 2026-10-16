package co.edu.unal.inventiq.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ExpirationStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

    @Test
    void aBatchWithoutDateNeverExpires() {
        assertThat(ExpirationStatus.classify(null, TODAY)).isEqualTo(ExpirationStatus.NO_EXPIRATION);
    }

    @Test
    void expiredIsFromTheDayAfterTheExpirationDate() {
        assertThat(ExpirationStatus.classify(TODAY.minusDays(1), TODAY)).isEqualTo(ExpirationStatus.EXPIRED);
        assertThat(ExpirationStatus.classify(TODAY, TODAY)).isEqualTo(ExpirationStatus.EXPIRING_SOON);
    }

    @Test
    void expiringSoonCoversTheNextSevenDays() {
        assertThat(ExpirationStatus.classify(TODAY.plusDays(ExpirationStatus.EXPIRING_SOON_DAYS), TODAY))
                .isEqualTo(ExpirationStatus.EXPIRING_SOON);
        assertThat(ExpirationStatus.classify(TODAY.plusDays(ExpirationStatus.EXPIRING_SOON_DAYS + 1), TODAY))
                .isEqualTo(ExpirationStatus.OK);
    }

    @Test
    void statusesHaveSpanishLabels() {
        assertThat(ExpirationStatus.EXPIRED.label()).isEqualTo("Vencido");
        assertThat(ExpirationStatus.EXPIRING_SOON.label()).isEqualTo("Por vencer");
        assertThat(ExpirationStatus.OK.label()).isEqualTo("Vigente");
        assertThat(ExpirationStatus.NO_EXPIRATION.label()).isEqualTo("Sin vencimiento");
    }

    @Test
    void aBatchKnowsItsStatusAndHowManyDaysAreLeft() {
        LocalDateTime received = LocalDateTime.of(2026, 10, 1, 8, 0);
        Batch soon = new Batch(1L, 1, 5, 5, TODAY.plusDays(3), received, 0);
        Batch expired = new Batch(2L, 1, 5, 5, TODAY.minusDays(2), received, 0);
        Batch noDate = new Batch(3L, 1, 5, 5, null, received, 0);

        assertThat(soon.expirationStatus(TODAY)).isEqualTo(ExpirationStatus.EXPIRING_SOON);
        assertThat(soon.daysUntilExpiration(TODAY)).isEqualTo(3L);
        assertThat(expired.daysUntilExpiration(TODAY)).isEqualTo(-2L);
        assertThat(noDate.expirationStatus(TODAY)).isEqualTo(ExpirationStatus.NO_EXPIRATION);
        assertThat(noDate.daysUntilExpiration(TODAY)).isNull();
    }
}
