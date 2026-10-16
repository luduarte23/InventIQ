package co.edu.unal.inventiq.application.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Totales de un período.
 *
 * @param grossMargin ingresos menos el costo vigente el día de cada venta (margen bruto estimado)
 */
public record SalesTotals(BigDecimal revenue, int units, int tickets, BigDecimal grossMargin) {

    static final SalesTotals EMPTY = new SalesTotals(BigDecimal.ZERO, 0, 0, BigDecimal.ZERO);

    public BigDecimal averageTicket() {
        return tickets == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(tickets), 0, RoundingMode.HALF_UP);
    }

    /** Margen bruto sobre los ingresos, en porcentaje con un decimal; null si no hubo ventas. */
    public BigDecimal marginPercent() {
        return revenue.signum() == 0 ? null : percent(grossMargin, revenue);
    }

    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        return part.multiply(BigDecimal.valueOf(100)).divide(whole, 1, RoundingMode.HALF_UP);
    }

    /** Variación porcentual frente a otro período, o null si el otro período no tuvo ventas. */
    static BigDecimal change(BigDecimal current, BigDecimal previous) {
        return previous.signum() == 0 ? null : percent(current.subtract(previous), previous);
    }
}
