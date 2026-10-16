package co.edu.unal.inventiq.application.reports;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.application.reports.SalesReport.DailySales;
import co.edu.unal.inventiq.application.reports.SalesReport.SaleSummary;
import co.edu.unal.inventiq.application.reports.SalesReport.WeekdaySales;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SaleItem;
import co.edu.unal.inventiq.fakes.InMemoryPriceHistoryRepository;
import co.edu.unal.inventiq.fakes.InMemoryProductRepository;
import co.edu.unal.inventiq.fakes.InMemorySaleRepository;

/**
 * Historial con fakes. Escenario (hoy es viernes 16 de octubre de 2026, período de 7 días: del
 * sábado 10 al viernes 16):
 * <ul>
 *   <li>Leche: costo 3.000 hasta el 12 de octubre y 3.300 desde entonces.</li>
 *   <li>Arroz: sin historial de precios; su margen usa el costo actual (3.600).</li>
 *   <li>Período anterior (3 al 9 de octubre): dos ventas por $ 13.800 en total.</li>
 * </ul>
 */
class SalesReportServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 16);

    private InMemorySaleRepository sales;
    private InMemoryProductRepository products;
    private InMemoryPriceHistoryRepository priceHistory;
    private SalesReportService service;

    private Product milk;
    private Product rice;
    private Product coffee;

    @BeforeEach
    void setUp() {
        sales = new InMemorySaleRepository();
        products = new InMemoryProductRepository();
        priceHistory = new InMemoryPriceHistoryRepository();
        Clock clock = Clock.fixed(TODAY.atTime(18, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        service = new SalesReportService(sales, products, priceHistory, clock);

        milk = products.create(aProduct().id(null).barcode("1").name("Leche").category(ProductCategory.LACTEOS)
                .salePrice(4500).cost(3300).build());
        rice = products.create(aProduct().id(null).barcode("2").name("Arroz").category(ProductCategory.GRANOS)
                .salePrice(4800).cost(3600).build());
        coffee = products.create(aProduct().id(null).barcode("3").name("Café").category(ProductCategory.CAFE)
                .salePrice(9800).cost(7000).build());
        priceHistory.add(milk.id(), 4000, 3000, LocalDateTime.of(2026, 9, 1, 8, 30));
        priceHistory.add(milk.id(), 4500, 3300, LocalDateTime.of(2026, 10, 12, 8, 0));

        sell(LocalDateTime.of(2026, 10, 11, 9, 30), item(milk, 2, 4000));                     // domingo, mañana
        sell(LocalDateTime.of(2026, 10, 13, 12, 0), item(milk, 1, 4500), item(rice, 3, 4800)); // martes, tarde
        sell(LocalDateTime.of(2026, 10, 16, 17, 0), item(rice, 1, 4800));                     // viernes, noche
        sell(LocalDateTime.of(2026, 10, 9, 11, 59), item(milk, 1, 4000));                     // período anterior
        sell(LocalDateTime.of(2026, 10, 3, 0, 0), item(coffee, 1, 9800));                     // período anterior
        sell(LocalDateTime.of(2026, 10, 2, 23, 59), item(coffee, 5, 9800));                    // fuera de ambos
    }

    @Test
    void totalsUseTheCostInForceOnTheDayOfEachSale() {
        SalesTotals totals = service.report(7).totals();

        assertThat(totals.revenue()).isEqualByComparingTo("31700");
        assertThat(totals.units()).isEqualTo(7);
        assertThat(totals.tickets()).isEqualTo(3);
        // Leche: 2×(4.000-3.000) + 1×(4.500-3.300); arroz: 4×(4.800-3.600).
        assertThat(totals.grossMargin()).isEqualByComparingTo("8000");
        assertThat(totals.marginPercent()).isEqualByComparingTo("25.2");
        assertThat(totals.averageTicket()).isEqualByComparingTo("10567");
    }

    @Test
    void theReportIsComparedWithThePreviousPeriodOfTheSameLength() {
        SalesReport report = service.report(7);

        assertThat(report.from()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(report.to()).isEqualTo(TODAY);
        assertThat(report.days()).isEqualTo(7);
        assertThat(report.previous().revenue()).isEqualByComparingTo("13800");
        assertThat(report.previous().tickets()).isEqualTo(2);
        assertThat(report.revenueChange()).isEqualByComparingTo("129.7");
        assertThat(report.unitsChange()).isEqualByComparingTo("250.0");
        assertThat(report.ticketsChange()).isEqualByComparingTo("50.0");
    }

    @Test
    void everyDayOfThePeriodHasAPointEvenWithoutSales() {
        List<DailySales> daily = service.report(7).daily();

        assertThat(daily).extracting(DailySales::date)
                .containsExactly(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 12),
                        LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 15), TODAY);
        assertThat(daily).extracting(DailySales::units).containsExactly(0, 2, 0, 4, 0, 0, 1);
        assertThat(daily.get(3).revenue()).isEqualByComparingTo("18900");
        assertThat(daily.get(3).tickets()).isEqualTo(1);
    }

    @Test
    void salesAreGroupedByTypeAndByProductFromHighestToLowestRevenue() {
        SalesReport report = service.report(7);

        assertThat(report.byCategory()).extracting(SalesReport.CategorySales::category)
                .containsExactly(ProductCategory.GRANOS, ProductCategory.LACTEOS);
        assertThat(report.byCategory().get(0).revenue()).isEqualByComparingTo("19200");
        assertThat(report.byCategory().get(0).units()).isEqualTo(4);

        assertThat(report.byProduct()).extracting(sale -> sale.product().name()).containsExactly("Arroz", "Leche");
        SalesReport.ProductSales riceSales = report.byProduct().get(0);
        assertThat(riceSales.grossMargin()).isEqualByComparingTo("4800");
        assertThat(riceSales.marginPercent()).isEqualByComparingTo("25.0");
        assertThat(riceSales.unitsPerDay(report.days())).isEqualByComparingTo("0.57");
    }

    @Test
    void weekdaysAreAveragedOverHowManyTimesTheyAppearInThePeriod() {
        List<WeekdaySales> week = service.report(14).byWeekday();

        assertThat(week).extracting(WeekdaySales::day).containsExactly(DayOfWeek.values());
        // Del sábado 3 al viernes 16 hay dos viernes: el 9 ($ 4.000) y el 16 ($ 4.800).
        WeekdaySales friday = week.get(DayOfWeek.FRIDAY.ordinal());
        assertThat(friday.occurrences()).isEqualTo(2);
        assertThat(friday.revenue()).isEqualByComparingTo("8800");
        assertThat(friday.averageRevenue()).isEqualByComparingTo("4400");
        WeekdaySales monday = week.get(DayOfWeek.MONDAY.ordinal());
        assertThat(monday.revenue()).isEqualByComparingTo("0");
        assertThat(monday.averageRevenue()).isEqualByComparingTo("0");
    }

    @Test
    void salesAreSplitIntoMorningAfternoonAndEvening() {
        List<SalesReport.TimeBandSales> bands = service.report(7).byTimeBand();

        assertThat(bands).extracting(SalesReport.TimeBandSales::band)
                .containsExactly(TimeBand.MORNING, TimeBand.AFTERNOON, TimeBand.EVENING);
        assertThat(bands).extracting(SalesReport.TimeBandSales::revenue)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("8000"), new BigDecimal("18900"), new BigDecimal("4800"));
    }

    @Test
    void timeBandsStartAtNoonAndAtFive() {
        assertThat(TimeBand.of(LocalTime.of(11, 59))).isEqualTo(TimeBand.MORNING);
        assertThat(TimeBand.of(LocalTime.NOON)).isEqualTo(TimeBand.AFTERNOON);
        assertThat(TimeBand.of(LocalTime.of(16, 59))).isEqualTo(TimeBand.AFTERNOON);
        assertThat(TimeBand.of(LocalTime.of(17, 0))).isEqualTo(TimeBand.EVENING);
        assertThat(TimeBand.MORNING.label()).isEqualTo("Mañana");
        assertThat(TimeBand.EVENING.range()).isEqualTo("desde las 5 p. m.");
    }

    @Test
    void recentSalesGoFromNewestToOldestAndAreLimited() {
        for (int i = 0; i < SalesReportService.RECENT_SALES + 5; i++) {
            sell(TODAY.atTime(8, 0).plusMinutes(i), item(rice, 1, 4800));
        }

        List<SaleSummary> recent = service.report(7).recentSales();

        assertThat(recent).hasSize(SalesReportService.RECENT_SALES);
        assertThat(recent.get(0).soldAt()).isEqualTo(TODAY.atTime(17, 0));
        assertThat(recent).isSortedAccordingTo((a, b) -> b.soldAt().compareTo(a.soldAt()));
    }

    @Test
    void aSaleSummaryCountsItsLinesUnitsAndTotal() {
        SaleSummary tuesday = service.report(7).recentSales().stream()
                .filter(sale -> sale.soldAt().getDayOfMonth() == 13).findFirst().orElseThrow();

        assertThat(tuesday.lines()).isEqualTo(2);
        assertThat(tuesday.units()).isEqualTo(4);
        assertThat(tuesday.total()).isEqualByComparingTo("18900");
    }

    @Test
    void aPeriodWithoutSalesHasZeroTotalsAndNothingToCompare() {
        InMemorySaleRepository empty = new InMemorySaleRepository();
        SalesReportService emptyService = new SalesReportService(empty, products, priceHistory,
                Clock.fixed(TODAY.atStartOfDay(BOGOTA).toInstant(), BOGOTA));

        SalesReport report = emptyService.report(30);

        assertThat(report.totals().tickets()).isZero();
        assertThat(report.totals().averageTicket()).isEqualByComparingTo("0");
        assertThat(report.totals().marginPercent()).isNull();
        assertThat(report.revenueChange()).isNull();
        assertThat(report.daily()).hasSize(30);
        assertThat(report.byProduct()).isEmpty();
    }

    @Test
    void aPeriodNeedsAtLeastOneDay() {
        assertThatThrownBy(() -> service.report(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pricesShowTheCurrentValuesTheirHistoryAndTheirChangeInThePeriod() {
        List<ProductPrices> prices = service.prices(7);

        assertThat(prices).extracting(p -> p.product().name()).containsExactly("Arroz", "Café", "Leche");
        ProductPrices milkPrices = prices.get(2);
        assertThat(milkPrices.history()).hasSize(2);
        assertThat(milkPrices.lastChange()).isEqualTo(LocalDateTime.of(2026, 10, 12, 8, 0));
        assertThat(milkPrices.costChangePercent()).isEqualByComparingTo("10.0");
        assertThat(milkPrices.priceChangePercent()).isEqualByComparingTo("12.5");
        assertThat(milkPrices.marginPercent()).isEqualByComparingTo("26.7");

        ProductPrices ricePrices = prices.get(0);
        assertThat(ricePrices.history()).isEmpty();
        assertThat(ricePrices.lastChange()).isNull();
        assertThat(ricePrices.costChangePercent()).isNull();
    }

    private void sell(LocalDateTime at, SaleItem... items) {
        sales.save(new Sale(null, "cajero", at, List.of(items)));
    }

    private static SaleItem item(Product product, int quantity, int price) {
        return new SaleItem(product.id(), 1, quantity, BigDecimal.valueOf(price));
    }
}
