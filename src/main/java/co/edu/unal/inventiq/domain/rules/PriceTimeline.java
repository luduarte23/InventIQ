package co.edu.unal.inventiq.domain.rules;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import co.edu.unal.inventiq.domain.model.PriceChange;

/**
 * La historia de precios de un producto: qué precio de venta y qué costo regían en cada momento.
 * Sirve para calcular el margen real de una venta pasada con el costo de ese día, no con el de hoy.
 */
public class PriceTimeline {

    private final List<PriceChange> changes;

    public PriceTimeline(List<PriceChange> changes) {
        this.changes = changes.stream().sorted(Comparator.comparing(PriceChange::changedAt)).toList();
    }

    /** Cambios en orden cronológico. */
    public List<PriceChange> changes() {
        return changes;
    }

    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public Optional<PriceChange> latest() {
        return changes.isEmpty() ? Optional.empty() : Optional.of(changes.get(changes.size() - 1));
    }

    /**
     * El precio vigente en ese momento: el último cambio hasta ese instante. Antes del primer
     * registro no hay datos más antiguos, así que se usa el primero.
     */
    public Optional<PriceChange> at(LocalDateTime moment) {
        PriceChange current = null;
        for (PriceChange change : changes) {
            if (change.changedAt().isAfter(moment)) {
                break;
            }
            current = change;
        }
        if (current == null && !changes.isEmpty()) {
            current = changes.get(0);
        }
        return Optional.ofNullable(current);
    }

    /** Costo vigente en ese momento, o {@code fallback} si el producto no tiene historial. */
    public BigDecimal costAt(LocalDateTime moment, BigDecimal fallback) {
        return at(moment).map(PriceChange::cost).orElse(fallback);
    }

    /** Variación porcentual del costo entre lo vigente en {@code since} y lo último, con un decimal. */
    public Optional<BigDecimal> costChangePercentSince(LocalDateTime since) {
        return changePercentSince(since, PriceChange::cost);
    }

    /** Variación porcentual del precio de venta entre lo vigente en {@code since} y lo último. */
    public Optional<BigDecimal> priceChangePercentSince(LocalDateTime since) {
        return changePercentSince(since, PriceChange::salePrice);
    }

    private Optional<BigDecimal> changePercentSince(LocalDateTime since, Function<PriceChange, BigDecimal> value) {
        Optional<PriceChange> start = at(since);
        Optional<PriceChange> end = latest();
        if (start.isEmpty() || end.isEmpty() || value.apply(start.get()).signum() == 0) {
            return Optional.empty();
        }
        BigDecimal before = value.apply(start.get());
        BigDecimal after = value.apply(end.get());
        return Optional.of(after.subtract(before).multiply(BigDecimal.valueOf(100))
                .divide(before, 1, RoundingMode.HALF_UP));
    }
}
