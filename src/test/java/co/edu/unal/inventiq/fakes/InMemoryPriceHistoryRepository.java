package co.edu.unal.inventiq.fakes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;

public class InMemoryPriceHistoryRepository implements PriceHistoryRepository {

    private final List<PriceChange> changes = new ArrayList<>();

    /** Atajo para las pruebas: registra un precio y costo vigentes desde {@code changedAt}. */
    public void add(long productId, int salePrice, int cost, LocalDateTime changedAt) {
        record(new PriceChange(null, productId, BigDecimal.valueOf(salePrice), BigDecimal.valueOf(cost), changedAt));
    }

    @Override
    public void record(PriceChange change) {
        changes.add(new PriceChange((long) changes.size() + 1, change.productId(), change.salePrice(), change.cost(),
                change.changedAt()));
    }

    @Override
    public List<PriceChange> findAll() {
        return changes.stream()
                .sorted(Comparator.comparingLong(PriceChange::productId).thenComparing(PriceChange::changedAt))
                .toList();
    }

    @Override
    public List<PriceChange> findByProduct(long productId) {
        return findAll().stream().filter(change -> change.productId() == productId).toList();
    }
}
