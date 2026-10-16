package co.edu.unal.inventiq.fakes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.port.BatchRepository;

/**
 * Se comporta como una base de datos: entrega copias y solo cambia el stock con saveAll,
 * que además verifica la versión (bloqueo optimista). Con {@link #failNextSaves(int)}
 * se simula que otro cajero modificó los lotes al mismo tiempo.
 */
public class InMemoryBatchRepository implements BatchRepository {

    private final Map<Long, Batch> stored = new LinkedHashMap<>();
    private long nextId = 1;
    private int pendingConflicts;
    private int saveCalls;

    public Batch add(long productId, int quantity, LocalDate expirationDate, LocalDateTime receivedAt) {
        return add(productId, quantity, quantity, expirationDate, receivedAt);
    }

    public Batch add(long productId, int received, int available, LocalDate expirationDate, LocalDateTime receivedAt) {
        Batch batch = new Batch(nextId++, productId, received, available, expirationDate, receivedAt, 0);
        stored.put(batch.getId(), batch);
        return copy(batch, 0);
    }

    /** Las próximas {@code times} llamadas a saveAll fallan como si hubiera un conflicto. */
    public void failNextSaves(int times) {
        pendingConflicts = times;
    }

    /** Simula que otro cajero vendió unidades de un lote (cambia cantidad y versión). */
    public void sellElsewhere(long batchId, int quantity) {
        Batch current = stored.get(batchId);
        Batch updated = copy(current, current.getVersion() + 1);
        updated.deduct(quantity);
        stored.put(batchId, updated);
    }

    public int quantityOf(long batchId) {
        return stored.get(batchId).getQuantityAvailable();
    }

    public int saveCalls() {
        return saveCalls;
    }

    @Override
    public List<Batch> findAvailableByProduct(long productId) {
        return stored.values().stream()
                .filter(b -> b.getProductId() == productId && b.getQuantityAvailable() > 0)
                .map(b -> copy(b, b.getVersion()))
                .toList();
    }

    @Override
    public List<Batch> findAllAvailable() {
        return stored.values().stream()
                .filter(b -> b.getQuantityAvailable() > 0)
                .map(b -> copy(b, b.getVersion()))
                .toList();
    }

    @Override
    public Batch create(Batch batch) {
        Batch saved = new Batch(nextId++, batch.getProductId(), batch.getQuantityReceived(),
                batch.getQuantityAvailable(), batch.getExpirationDate(), batch.getReceivedAt(), 0);
        stored.put(saved.getId(), saved);
        return copy(saved, 0);
    }

    public List<Batch> all() {
        return stored.values().stream().map(b -> copy(b, b.getVersion())).toList();
    }

    @Override
    public void saveAll(List<Batch> batches) {
        saveCalls++;
        if (pendingConflicts > 0) {
            pendingConflicts--;
            throw new ConcurrentStockUpdateException(null);
        }
        for (Batch batch : batches) {
            if (stored.get(batch.getId()).getVersion() != batch.getVersion()) {
                throw new ConcurrentStockUpdateException(null);
            }
        }
        for (Batch batch : batches) {
            stored.put(batch.getId(), copy(batch, batch.getVersion() + 1));
        }
    }

    private static Batch copy(Batch b, long version) {
        return new Batch(b.getId(), b.getProductId(), b.getQuantityReceived(), b.getQuantityAvailable(),
                b.getExpirationDate(), b.getReceivedAt(), version);
    }
}
