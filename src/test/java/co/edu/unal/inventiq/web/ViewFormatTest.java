package co.edu.unal.inventiq.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.application.sales.BatchView;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;

class ViewFormatTest {

    private final ViewFormat fmt = new ViewFormat();

    @Test
    void pesosAreFormattedTheColombianWay() {
        assertThat(fmt.money(new BigDecimal("12500"))).isEqualTo("$ 12.500");
        assertThat(fmt.money(null)).isEqualTo("—");
        assertThat(fmt.number(1284)).isEqualTo("1.284");
    }

    @Test
    void indicatorsUseCompactAmounts() {
        assertThat(fmt.compactMoney(new BigDecimal("4523000"))).isEqualTo("$ 4,5 M");
        assertThat(fmt.compactMoney(new BigDecimal("845300"))).isEqualTo("$ 845 mil");
        assertThat(fmt.compactMoney(new BigDecimal("6400"))).isEqualTo("$ 6.400");
    }

    @Test
    void percentagesHaveADecimalCommaAndChangesASign() {
        assertThat(fmt.percent(new BigDecimal("26.7"))).isEqualTo("26,7 %");
        assertThat(fmt.signedPercent(new BigDecimal("8.3"))).isEqualTo("+8,3 %");
        assertThat(fmt.signedPercent(new BigDecimal("-2.1"))).isEqualTo("-2,1 %");
        assertThat(fmt.signedPercent(null)).isEqualTo("—");
    }

    @Test
    void datesUseSpanishNames() {
        LocalDate date = LocalDate.of(2026, 10, 16);

        assertThat(fmt.month(date)).isEqualTo("oct");
        assertThat(fmt.date(date)).isEqualTo("16 oct 2026");
        assertThat(fmt.shortDate(date)).isEqualTo("16 oct");
        assertThat(fmt.longDate(date)).isEqualTo("viernes, 16 de octubre de 2026");
        assertThat(fmt.time(LocalDateTime.of(2026, 10, 16, 8, 5))).isEqualTo("08:05");
        assertThat(fmt.weekdayShort(DayOfWeek.WEDNESDAY)).isEqualTo("Mié");
    }

    @Test
    void expirationIsExplainedInWords() {
        assertThat(fmt.expiration(view(-2L, ExpirationStatus.EXPIRED))).isEqualTo("Vencido hace 2 días");
        assertThat(fmt.expiration(view(-1L, ExpirationStatus.EXPIRED))).isEqualTo("Vencido hace 1 día");
        assertThat(fmt.expiration(view(0L, ExpirationStatus.EXPIRING_SOON))).isEqualTo("Vence hoy");
        assertThat(fmt.expiration(view(1L, ExpirationStatus.EXPIRING_SOON))).isEqualTo("Vence mañana");
        assertThat(fmt.expiration(view(3L, ExpirationStatus.EXPIRING_SOON))).isEqualTo("Vence en 3 días");
        assertThat(fmt.expiration(view(null, ExpirationStatus.NO_EXPIRATION))).isEqualTo("Sin vencimiento");
        assertThat(fmt.statusClass(ExpirationStatus.EXPIRED)).isEqualTo("expired");
        assertThat(fmt.statusClass(null)).isEqualTo("none");
    }

    private static BatchView view(Long days, ExpirationStatus status) {
        Batch batch = new Batch(1L, 1, 5, 5, null, LocalDateTime.of(2026, 10, 1, 8, 0), 0);
        return new BatchView(batch, status, days);
    }
}
