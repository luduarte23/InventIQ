package co.edu.unal.inventiq.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.application.sales.BatchView;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/**
 * Formatos en español de Colombia para las plantillas: {@code ${@fmt.money(valor)}}. Los nombres
 * de meses y días van fijos para no depender de la versión de los datos de idioma de Java.
 */
@Component("fmt")
public class ViewFormat {

    private static final String[] MONTHS = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct",
            "nov", "dic"};
    private static final String[] MONTHS_LONG = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] WEEKDAYS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado",
            "domingo"};
    private static final String[] WEEKDAYS_SHORT = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};
    private static final DecimalFormatSymbols COLOMBIA = DecimalFormatSymbols.getInstance(Locale.of("es", "CO"));
    private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);
    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    /** "$ 12.500" */
    public String money(BigDecimal amount) {
        return amount == null ? "—" : Money.format(amount);
    }

    /** Valor compacto para indicadores: "$ 4,5 M", "$ 845 mil", "$ 6.400". */
    public String compactMoney(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        BigDecimal absolute = amount.abs();
        if (absolute.compareTo(MILLION) >= 0) {
            return "$ " + decimal(amount.divide(MILLION, 1, RoundingMode.HALF_UP)) + " M";
        }
        if (absolute.compareTo(TEN_THOUSAND) >= 0) {
            return "$ " + number(amount.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)) + " mil";
        }
        return money(amount);
    }

    /** "1.284" */
    public String number(Number value) {
        return value == null ? "—" : new DecimalFormat("#,##0", COLOMBIA).format(value);
    }

    /** "8,3" (hasta un decimal) */
    public String decimal(BigDecimal value) {
        return value == null ? "—" : new DecimalFormat("#,##0.#", COLOMBIA).format(value);
    }

    /** "8,3 %" */
    public String percent(Number value) {
        if (value == null) {
            return "—";
        }
        return decimal(new BigDecimal(value.toString())) + " %";
    }

    /** "+8,3 %" o "-2,1 %": variación frente a otro período. */
    public String signedPercent(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        return (value.signum() > 0 ? "+" : "") + decimal(value) + " %";
    }

    /** "oct" */
    public String month(LocalDate date) {
        return date == null ? "—" : MONTHS[date.getMonthValue() - 1];
    }

    /** "16 oct 2026" */
    public String date(LocalDate date) {
        return date == null ? "—" : date.getDayOfMonth() + " " + month(date) + " " + date.getYear();
    }

    /** "16 oct" */
    public String shortDate(LocalDate date) {
        return date == null ? "—" : date.getDayOfMonth() + " " + month(date);
    }

    /** "15:45" */
    public String time(LocalDateTime moment) {
        return moment == null ? "—" : String.format("%02d:%02d", moment.getHour(), moment.getMinute());
    }

    /** "16 oct 2026, 15:45" */
    public String dateTime(LocalDateTime moment) {
        return moment == null ? "—" : date(moment.toLocalDate()) + ", " + time(moment);
    }

    /** "viernes, 16 de octubre de 2026" */
    public String longDate(LocalDate date) {
        if (date == null) {
            return "—";
        }
        return WEEKDAYS[date.getDayOfWeek().getValue() - 1] + ", " + date.getDayOfMonth() + " de "
                + MONTHS_LONG[date.getMonthValue() - 1] + " de " + date.getYear();
    }

    /** "Lun" */
    public String weekdayShort(DayOfWeek day) {
        return WEEKDAYS_SHORT[day.getValue() - 1];
    }

    /** "lunes" */
    public String weekday(DayOfWeek day) {
        return WEEKDAYS[day.getValue() - 1];
    }

    /** "Vencido hace 2 días", "Vence hoy", "Vence en 3 días", "Sin vencimiento". */
    public String expiration(BatchView view) {
        if (view == null || view.daysUntilExpiration() == null) {
            return "Sin vencimiento";
        }
        long days = view.daysUntilExpiration();
        if (days < 0) {
            return "Vencido hace " + plural(-days, "día", "días");
        }
        if (days == 0) {
            return "Vence hoy";
        }
        if (days == 1) {
            return "Vence mañana";
        }
        return "Vence en " + plural(days, "día", "días");
    }

    /** Clase CSS del estado: expired, soon, ok o none. */
    public String statusClass(ExpirationStatus status) {
        if (status == null) {
            return "none";
        }
        return switch (status) {
            case EXPIRED -> "expired";
            case EXPIRING_SOON -> "soon";
            case OK -> "ok";
            case NO_EXPIRATION -> "none";
        };
    }

    /** "1 unidad", "3 unidades" */
    public String plural(long count, String singular, String pluralForm) {
        return number(count) + " " + (count == 1 ? singular : pluralForm);
    }
}
