package co.edu.unal.inventiq.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.application.sales.BatchView;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/** Formatos en español para las plantillas: {@code ${@fmt.money(valor)}}. */
@Component("fmt")
public class ViewFormat {

    private static final String[] MONTHS = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct",
            "nov", "dic"};
    private static final String[] MONTHS_LONG = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final String[] WEEKDAYS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado",
            "domingo"};

    public String money(BigDecimal amount) {
        return amount == null ? "—" : Money.format(amount);
    }

    public String number(Number value) {
        return new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.of("es", "CO"))).format(value);
    }

    /** "oct" */
    public String month(LocalDate date) {
        return MONTHS[date.getMonthValue() - 1];
    }

    /** "16 oct 2026" */
    public String date(LocalDate date) {
        return date.getDayOfMonth() + " " + month(date) + " " + date.getYear();
    }

    /** "viernes, 16 de octubre de 2026" */
    public String longDate(LocalDate date) {
        return WEEKDAYS[date.getDayOfWeek().getValue() - 1] + ", " + date.getDayOfMonth() + " de "
                + MONTHS_LONG[date.getMonthValue() - 1] + " de " + date.getYear();
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
        return days == 0 ? "Vence hoy" : days == 1 ? "Vence mañana" : "Vence en " + plural(days, "día", "días");
    }

    /** Clase CSS del estado: expired, soon, ok o none. */
    public String statusClass(ExpirationStatus status) {
        return switch (status) {
            case EXPIRED -> "expired";
            case EXPIRING_SOON -> "soon";
            case OK -> "ok";
            case NO_EXPIRATION -> "none";
        };
    }

    /** "1 lote", "3 lotes" */
    public String plural(long count, String singular, String pluralForm) {
        return number(count) + " " + (count == 1 ? singular : pluralForm);
    }
}
