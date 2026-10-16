package co.edu.unal.inventiq.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.unal.inventiq.application.reports.ProductPrices;
import co.edu.unal.inventiq.application.reports.SalesReport;
import co.edu.unal.inventiq.application.reports.SalesReport.ProductSales;
import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.web.ViewFormat;

/**
 * Los datos de las gráficas del historial, como mapas y listas de números y textos: la plantilla
 * los inserta en la página como JSON y {@code static/js/history-charts.js} los dibuja.
 */
final class HistoryChartData {

    static final int TOP_PRODUCTS = 10;

    private HistoryChartData() {
    }

    static Map<String, Object> build(SalesReport report, List<ProductPrices> prices, Long selectedProduct,
                                     ViewFormat fmt) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days", report.days());
        data.put("daily", daily(report, fmt));
        data.put("categories", categories(report));
        data.put("topByRevenue", topProducts(report, Comparator.comparing(ProductSales::revenue), false));
        data.put("topByUnits", topProducts(report, Comparator.comparingInt(ProductSales::units), true));
        data.put("weekdays", weekdays(report, fmt));
        data.put("timeBands", timeBands(report));
        data.put("performance", performance(report));
        data.put("prices", prices(prices, report.to(), fmt));
        data.put("selectedProduct", selectedProduct);
        return data;
    }

    private static Map<String, Object> daily(SalesReport report, ViewFormat fmt) {
        Map<String, Object> daily = new LinkedHashMap<>();
        daily.put("labels", report.daily().stream().map(day -> fmt.shortDate(day.date())).toList());
        daily.put("dates", report.daily().stream().map(day -> fmt.date(day.date())).toList());
        daily.put("revenue", report.daily().stream().map(SalesReport.DailySales::revenue).toList());
        daily.put("units", report.daily().stream().map(SalesReport.DailySales::units).toList());
        daily.put("tickets", report.daily().stream().map(SalesReport.DailySales::tickets).toList());
        return daily;
    }

    private static Map<String, Object> categories(SalesReport report) {
        Map<String, Object> categories = new LinkedHashMap<>();
        categories.put("labels", report.byCategory().stream().map(c -> c.category().label()).toList());
        categories.put("revenue", report.byCategory().stream().map(SalesReport.CategorySales::revenue).toList());
        categories.put("units", report.byCategory().stream().map(SalesReport.CategorySales::units).toList());
        return categories;
    }

    private static Map<String, Object> topProducts(SalesReport report, Comparator<ProductSales> order, boolean units) {
        List<ProductSales> top = report.byProduct().stream()
                .sorted(order.reversed().thenComparing(sale -> sale.product().name()))
                .limit(TOP_PRODUCTS)
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", top.stream().map(sale -> sale.product().name()).toList());
        result.put("values", top.stream().map(sale -> units ? (Object) sale.units() : sale.revenue()).toList());
        return result;
    }

    private static Map<String, Object> weekdays(SalesReport report, ViewFormat fmt) {
        Map<String, Object> weekdays = new LinkedHashMap<>();
        weekdays.put("labels", report.byWeekday().stream().map(day -> fmt.weekdayShort(day.day())).toList());
        weekdays.put("names", report.byWeekday().stream().map(day -> fmt.weekday(day.day())).toList());
        weekdays.put("average", report.byWeekday().stream().map(SalesReport.WeekdaySales::averageRevenue).toList());
        weekdays.put("tickets", report.byWeekday().stream().map(SalesReport.WeekdaySales::tickets).toList());
        return weekdays;
    }

    private static Map<String, Object> timeBands(SalesReport report) {
        Map<String, Object> bands = new LinkedHashMap<>();
        bands.put("labels", report.byTimeBand().stream().map(b -> b.band().label()).toList());
        bands.put("ranges", report.byTimeBand().stream().map(b -> b.band().range()).toList());
        bands.put("revenue", report.byTimeBand().stream().map(SalesReport.TimeBandSales::revenue).toList());
        bands.put("tickets", report.byTimeBand().stream().map(SalesReport.TimeBandSales::tickets).toList());
        return bands;
    }

    /** Un punto por producto vendido: rotación (unidades/día), margen y ingresos. */
    private static List<Map<String, Object>> performance(SalesReport report) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (ProductSales sale : report.byProduct()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("name", sale.product().name());
            point.put("category", sale.product().category().label());
            point.put("unitsPerDay", sale.unitsPerDay(report.days()));
            point.put("margin", sale.marginPercent() == null ? BigDecimal.ZERO : sale.marginPercent());
            point.put("revenue", sale.revenue());
            points.add(point);
        }
        return points;
    }

    /**
     * La historia de precios de cada producto. Se agrega un punto final hoy con el último precio,
     * para que la línea escalonada llegue hasta la fecha actual.
     */
    private static List<Map<String, Object>> prices(List<ProductPrices> prices, LocalDate today, ViewFormat fmt) {
        List<Map<String, Object>> products = new ArrayList<>();
        for (ProductPrices product : prices) {
            List<Map<String, Object>> points = new ArrayList<>();
            for (PriceChange change : product.history()) {
                points.add(point(change.changedAt().toLocalDate(), change.salePrice(), change.cost(), true, fmt));
            }
            PriceChange last = product.history().isEmpty() ? null : product.history().get(product.history().size() - 1);
            if (last == null) {
                points.add(point(today, product.product().salePrice(), product.product().cost(), false, fmt));
            } else if (last.changedAt().toLocalDate().isBefore(today)) {
                points.add(point(today, last.salePrice(), last.cost(), false, fmt));
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", product.product().id());
            entry.put("name", product.product().name());
            entry.put("points", points);
            products.add(entry);
        }
        return products;
    }

    /** @param change true si es un cambio registrado; false si es el punto de hoy que extiende la línea */
    private static Map<String, Object> point(LocalDate date, BigDecimal price, BigDecimal cost, boolean change,
                                             ViewFormat fmt) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("x", date.toEpochDay());
        point.put("label", fmt.date(date));
        point.put("price", price);
        point.put("cost", cost);
        point.put("change", change);
        return point;
    }
}
