package co.edu.unal.inventiq.application.reports;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unal.inventiq.application.reports.SalesReport.CategorySales;
import co.edu.unal.inventiq.application.reports.SalesReport.DailySales;
import co.edu.unal.inventiq.application.reports.SalesReport.ProductSales;
import co.edu.unal.inventiq.application.reports.SalesReport.SaleSummary;
import co.edu.unal.inventiq.application.reports.SalesReport.TimeBandSales;
import co.edu.unal.inventiq.application.reports.SalesReport.WeekdaySales;
import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.SoldItem;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SaleRepository;
import co.edu.unal.inventiq.domain.rules.PriceTimeline;

/**
 * Historial de ventas y de precios (Administración → Historial). El margen de cada venta se
 * calcula con el costo vigente ese día (PriceTimeline), no con el costo de hoy.
 */
public class SalesReportService {

    /** Períodos que ofrece la pantalla, en días. */
    public static final List<Integer> PERIODS = List.of(7, 30, 90);
    static final int RECENT_SALES = 30;

    private final SaleRepository sales;
    private final ProductRepository products;
    private final PriceHistoryRepository priceHistory;
    private final Clock clock;

    public SalesReportService(SaleRepository sales, ProductRepository products, PriceHistoryRepository priceHistory,
                              Clock clock) {
        this.sales = sales;
        this.products = products;
        this.priceHistory = priceHistory;
        this.clock = clock;
    }

    /** Los últimos {@code days} días, incluido hoy, comparados con los {@code days} días anteriores. */
    public SalesReport report(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("El período debe tener al menos un día");
        }
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(days - 1L);
        LocalDateTime start = from.atStartOfDay();
        List<SoldItem> items = sales.findItemsSoldBetween(start, to.plusDays(1).atStartOfDay());
        List<SoldItem> previousItems = sales.findItemsSoldBetween(start.minusDays(days), start);

        Map<Long, Product> catalog = products.findAll().stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));
        Map<Long, PriceTimeline> timelines = timelines();
        Pricing pricing = new Pricing(catalog, timelines);

        return new SalesReport(from, to,
                totals(items, pricing),
                totals(previousItems, pricing),
                daily(items, from, to),
                byCategory(items, catalog),
                byProduct(items, catalog, pricing),
                byWeekday(items, from, to),
                byTimeBand(items),
                recentSales(items));
    }

    /** Precios actuales, su historia y la variación de los últimos {@code days} días, por nombre. */
    public List<ProductPrices> prices(int days) {
        Map<Long, PriceTimeline> timelines = timelines();
        LocalDateTime since = LocalDate.now(clock).minusDays(days).atStartOfDay();
        return products.findAll().stream()
                .map(product -> {
                    PriceTimeline timeline = timelines.getOrDefault(product.id(), new PriceTimeline(List.of()));
                    return new ProductPrices(product, timeline.changes(),
                            timeline.costChangePercentSince(since).orElse(null),
                            timeline.priceChangePercentSince(since).orElse(null));
                })
                .toList();
    }

    private Map<Long, PriceTimeline> timelines() {
        return priceHistory.findAll().stream()
                .collect(Collectors.groupingBy(PriceChange::productId,
                        Collectors.collectingAndThen(Collectors.toList(), PriceTimeline::new)));
    }

    private static SalesTotals totals(List<SoldItem> items, Pricing pricing) {
        if (items.isEmpty()) {
            return SalesTotals.EMPTY;
        }
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal margin = BigDecimal.ZERO;
        int units = 0;
        Set<Long> tickets = new HashSet<>();
        for (SoldItem item : items) {
            revenue = revenue.add(item.revenue());
            margin = margin.add(pricing.margin(item));
            units += item.quantity();
            tickets.add(item.saleId());
        }
        return new SalesTotals(revenue, units, tickets.size(), margin);
    }

    /** Un punto por día del período, también los días sin ventas (en cero). */
    private static List<DailySales> daily(List<SoldItem> items, LocalDate from, LocalDate to) {
        Map<LocalDate, List<SoldItem>> byDate = items.stream()
                .collect(Collectors.groupingBy(item -> item.soldAt().toLocalDate()));
        List<DailySales> daily = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            List<SoldItem> dayItems = byDate.getOrDefault(date, List.of());
            daily.add(new DailySales(date, revenue(dayItems), units(dayItems), tickets(dayItems)));
        }
        return daily;
    }

    private static List<CategorySales> byCategory(List<SoldItem> items, Map<Long, Product> catalog) {
        Map<ProductCategory, List<SoldItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(item -> categoryOf(item, catalog), LinkedHashMap::new,
                        Collectors.toList()));
        return grouped.entrySet().stream()
                .map(entry -> new CategorySales(entry.getKey(), revenue(entry.getValue()), units(entry.getValue())))
                .sorted(Comparator.comparing(CategorySales::revenue).reversed())
                .toList();
    }

    private static List<ProductSales> byProduct(List<SoldItem> items, Map<Long, Product> catalog, Pricing pricing) {
        Map<Long, List<SoldItem>> grouped = items.stream().collect(Collectors.groupingBy(SoldItem::productId));
        return grouped.entrySet().stream()
                .filter(entry -> catalog.containsKey(entry.getKey()))
                .map(entry -> new ProductSales(catalog.get(entry.getKey()), revenue(entry.getValue()),
                        units(entry.getValue()),
                        entry.getValue().stream().map(pricing::margin).reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(ProductSales::revenue).reversed()
                        .thenComparing(sale -> sale.product().name()))
                .toList();
    }

    /** De lunes a domingo, con cuántas veces cae cada día en el período para sacar el promedio. */
    private static List<WeekdaySales> byWeekday(List<SoldItem> items, LocalDate from, LocalDate to) {
        Map<DayOfWeek, Integer> occurrences = new EnumMap<>(DayOfWeek.class);
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            occurrences.merge(date.getDayOfWeek(), 1, Integer::sum);
        }
        Map<DayOfWeek, List<SoldItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(item -> item.soldAt().getDayOfWeek()));
        List<WeekdaySales> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            List<SoldItem> dayItems = grouped.getOrDefault(day, List.of());
            result.add(new WeekdaySales(day, revenue(dayItems), tickets(dayItems), occurrences.getOrDefault(day, 0)));
        }
        return result;
    }

    private static List<TimeBandSales> byTimeBand(List<SoldItem> items) {
        Map<TimeBand, List<SoldItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(item -> TimeBand.of(item.soldAt().toLocalTime())));
        List<TimeBandSales> result = new ArrayList<>();
        for (TimeBand band : TimeBand.values()) {
            List<SoldItem> bandItems = grouped.getOrDefault(band, List.of());
            result.add(new TimeBandSales(band, revenue(bandItems), tickets(bandItems)));
        }
        return result;
    }

    /** Las últimas ventas del período, de la más reciente a la más antigua. */
    private static List<SaleSummary> recentSales(List<SoldItem> items) {
        Map<Long, List<SoldItem>> bySale = items.stream().collect(Collectors.groupingBy(SoldItem::saleId));
        return bySale.entrySet().stream()
                .map(entry -> new SaleSummary(entry.getKey(), entry.getValue().get(0).soldAt(),
                        entry.getValue().size(), units(entry.getValue()), revenue(entry.getValue())))
                .sorted(Comparator.comparing(SaleSummary::soldAt).reversed()
                        .thenComparing(Comparator.comparing(SaleSummary::saleId).reversed()))
                .limit(RECENT_SALES)
                .toList();
    }

    private static ProductCategory categoryOf(SoldItem item, Map<Long, Product> catalog) {
        Product product = catalog.get(item.productId());
        return product == null ? ProductCategory.OTROS : product.category();
    }

    private static BigDecimal revenue(List<SoldItem> items) {
        return items.stream().map(SoldItem::revenue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int units(List<SoldItem> items) {
        return items.stream().mapToInt(SoldItem::quantity).sum();
    }

    private static int tickets(List<SoldItem> items) {
        return (int) items.stream().map(SoldItem::saleId).distinct().count();
    }

    /** Costo vigente el día de cada venta, con el costo actual como respaldo si no hay historial. */
    private record Pricing(Map<Long, Product> catalog, Map<Long, PriceTimeline> timelines) {

        BigDecimal margin(SoldItem item) {
            Product product = catalog.get(item.productId());
            BigDecimal fallback = product == null ? item.unitPrice() : product.cost();
            PriceTimeline timeline = timelines.get(item.productId());
            BigDecimal cost = timeline == null ? fallback : timeline.costAt(item.soldAt(), fallback);
            return item.unitPrice().subtract(cost).multiply(BigDecimal.valueOf(item.quantity()));
        }
    }
}
