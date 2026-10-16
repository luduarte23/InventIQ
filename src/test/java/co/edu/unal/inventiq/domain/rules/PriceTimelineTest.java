package co.edu.unal.inventiq.domain.rules;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.model.PriceChange;

class PriceTimelineTest {

    private static final LocalDateTime MAY = LocalDateTime.of(2026, 5, 1, 8, 30);
    private static final LocalDateTime JULY = LocalDateTime.of(2026, 7, 1, 8, 30);
    private static final LocalDateTime SEPTEMBER = LocalDateTime.of(2026, 9, 1, 8, 30);

    /** Desordenado a propósito: la línea de tiempo los ordena. */
    private final PriceTimeline timeline = new PriceTimeline(List.of(
            change(4800, 3500, SEPTEMBER),
            change(4000, 3000, MAY),
            change(4400, 3200, JULY)));

    @Test
    void changesAreKeptInChronologicalOrder() {
        assertThat(timeline.changes()).extracting(PriceChange::changedAt).containsExactly(MAY, JULY, SEPTEMBER);
        assertThat(timeline.latest()).hasValueSatisfying(change -> assertThat(change.changedAt()).isEqualTo(SEPTEMBER));
    }

    @Test
    void thePriceInForceIsTheLastChangeUpToThatMoment() {
        assertThat(timeline.at(JULY.plusDays(10))).hasValueSatisfying(change ->
                assertThat(change.salePrice()).isEqualByComparingTo("4400"));
        assertThat(timeline.at(JULY)).hasValueSatisfying(change ->
                assertThat(change.salePrice()).isEqualByComparingTo("4400"));
        assertThat(timeline.at(JULY.minusSeconds(1))).hasValueSatisfying(change ->
                assertThat(change.salePrice()).isEqualByComparingTo("4000"));
    }

    @Test
    void beforeTheFirstRecordTheFirstPriceIsUsed() {
        assertThat(timeline.at(MAY.minusYears(1))).hasValueSatisfying(change ->
                assertThat(change.salePrice()).isEqualByComparingTo("4000"));
    }

    @Test
    void theCostOfASaleIsTheOneInForceThatDay() {
        assertThat(timeline.costAt(JULY.plusDays(1), BigDecimal.ONE)).isEqualByComparingTo("3200");
        assertThat(new PriceTimeline(List.of()).costAt(JULY, new BigDecimal("999"))).isEqualByComparingTo("999");
    }

    @Test
    void changePercentCompareWhatWasInForceThenWithTheLatest() {
        assertThat(timeline.costChangePercentSince(JULY.plusDays(1))).hasValueSatisfying(percent ->
                assertThat(percent).isEqualByComparingTo("9.4"));
        assertThat(timeline.priceChangePercentSince(MAY.plusDays(1))).hasValueSatisfying(percent ->
                assertThat(percent).isEqualByComparingTo("20.0"));
        assertThat(timeline.priceChangePercentSince(SEPTEMBER.plusDays(1))).hasValueSatisfying(percent ->
                assertThat(percent).isEqualByComparingTo("0"));
    }

    @Test
    void withoutHistoryThereIsNothingToCompare() {
        PriceTimeline empty = new PriceTimeline(List.of());

        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.at(JULY)).isEmpty();
        assertThat(empty.latest()).isEmpty();
        assertThat(empty.costChangePercentSince(JULY)).isEmpty();
    }

    @Test
    void aZeroStartingValueCannotBeCompared() {
        PriceTimeline fromZero = new PriceTimeline(List.of(change(1000, 0, MAY), change(1200, 500, JULY)));

        assertThat(fromZero.costChangePercentSince(MAY)).isEmpty();
        assertThat(fromZero.priceChangePercentSince(MAY)).hasValueSatisfying(percent ->
                assertThat(percent).isEqualByComparingTo("20.0"));
    }

    private static PriceChange change(int price, int cost, LocalDateTime at) {
        return new PriceChange(null, 1L, BigDecimal.valueOf(price), BigDecimal.valueOf(cost), at);
    }
}
