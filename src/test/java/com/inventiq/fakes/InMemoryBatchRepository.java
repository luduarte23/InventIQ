package com.inventiq.fakes;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.port.BatchRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso de lotes: guarda todo en una lista. Solo para pruebas. */
public class InMemoryBatchRepository implements BatchRepository {

    private final List<Batch> batches = new ArrayList<>();

    /** Lotes con unidades y no vencidos. No los ordena: el orden FIFO lo pone FifoAllocator. */
    @Override
    public List<Batch> findAvailableByProduct(Long productId, LocalDate today) {
        List<Batch> available = new ArrayList<>();
        for (Batch batch : batches) {
            if (batch.getProductId().equals(productId) && batch.getQuantityRemaining() > 0
                    && !batch.isExpired(today)) {
                available.add(batch);
            }
        }
        return available;
    }

    @Override
    public Optional<Batch> findById(Long id) {
        for (Batch batch : batches) {
            if (batch.getId().equals(id)) {
                return Optional.of(batch);
            }
        }
        return Optional.empty();
    }

    @Override
    public Batch save(Batch batch) {
        if (!batches.contains(batch)) {
            batches.add(batch);
        }
        return batch;
    }

    // La venta no usa estos dos métodos.
    @Override
    public int availableStock(Long productId, LocalDate today) {
        return 0;
    }

    @Override
    public List<Batch> findExpiringBefore(LocalDate limit) {
        return List.of();
    }
}
