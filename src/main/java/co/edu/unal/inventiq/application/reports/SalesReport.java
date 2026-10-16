package co.edu.unal.inventiq.application.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;

/**
 * Historial de ventas de un período [from, to], con el período anterior de igual duración para
 * comparar.
 *
 * @param byProduct todos los productos vendidos, de más a menos ingresos
 */
public record SalesReport(
        LocalDate from,
        LocalDate to,
        SalesTotals totals,
        SalesTotals previous,
        List<DailySales> daily,
        List<CategorySales> byCategory,
        List<ProductSales> byProduct,
        List<WeekdaySales> byWeekday,
        List<TimeBandSales> byTimeBand,
        List<SaleSummary> recentSales) {

    public int days() {
        return (int) ChronoUnit.DAYS.between(from, to) + 1;
    }

    /** Variación de ingresos frente al período anterior, en porcentaje; null si no hay con qué comparar. */
    public BigDecimal revenueChange() {
        return SalesTotals.change(totals.revenue(), previous.revenue());
    }

    public BigDecimal unitsChange() {
        return SalesTotals.change(BigDecimal.valueOf(totals.units()), BigDecimal.valueOf(previous.units()));
    }

    public BigDecimal ticketsChange() {
        return SalesTotals.change(BigDecimal.valueOf(totals.tickets()), BigDecimal.valueOf(previous.tickets()));
    }

    public record DailySales(LocalDate date, BigDecimal revenue, int units, int tickets) {
    }

    public record CategorySales(ProductCategory category, BigDecimal revenue, int units) {
    }

    public record ProductSales(Product product, BigDecimal revenue, int units, BigDecimal grossMargin) {

        public BigDecimal marginPercent() {
            return revenue.signum() == 0 ? null : SalesTotals.percent(grossMargin, revenue);
        }

        /** Rotación: unidades vendidas por día del período. */
        public BigDecimal unitsPerDay(int days) {
            return BigDecimal.valueOf(units).divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
        }
    }

    /** @param occurrences cuántas veces aparece ese día de la semana en el período */
    public record WeekdaySales(DayOfWeek day, BigDecimal revenue, int tickets, int occurrences) {

        /** Ingresos promedio de ese día de la semana. */
        public BigDecimal averageRevenue() {
            return occurrences == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(occurrences), 0, RoundingMode.HALF_UP);
        }
    }

    public record TimeBandSales(TimeBand band, BigDecimal revenue, int tickets) {
    }

    public record SaleSummary(long saleId, LocalDateTime soldAt, int lines, int units, BigDecimal total) {
    }
}
