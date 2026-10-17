package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Contenido neto: cuánto trae el empaque, por ejemplo 500 g, 1,5 L o 6 und. */
public record NetContent(BigDecimal amount, ContentUnit unit) {

    public NetContent {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(unit, "unit");
    }

    /** "500 g", "1,5 L", "6 und": sin ceros de más y con coma decimal. */
    public String display() {
        return amount.stripTrailingZeros().toPlainString().replace('.', ',') + " " + unit.symbol();
    }
}
