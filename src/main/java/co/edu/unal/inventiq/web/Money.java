package co.edu.unal.inventiq.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formato de pesos colombianos sin decimales: "$ 12.500". Es el mismo que usan las
 * plantillas con #numbers.formatDecimal(valor, 1, 'POINT', 0, 'COMMA').
 */
public final class Money {

    private static final DecimalFormatSymbols COLOMBIA = DecimalFormatSymbols.getInstance(Locale.of("es", "CO"));

    private Money() {
    }

    public static String format(BigDecimal amount) {
        return "$ " + new DecimalFormat("#,##0", COLOMBIA).format(amount);
    }
}
