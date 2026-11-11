package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.rules.FifoAllocator;
import com.inventiq.infrastructure.persistence.entity.BatchEntity;
import com.inventiq.infrastructure.persistence.jpa.BatchJpaRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa el puerto BatchRepository del dominio usando JPA. */
@Repository
public class BatchRepositoryAdapter implements BatchRepository {

    private final BatchJpaRepository jpa;

    public BatchRepositoryAdapter(BatchJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Lotes no vencidos y con unidades, en orden FIFO (el orden lo define la regla del dominio). */
    @Override
    public List<Batch> findAvailableByProduct(Long productId, LocalDate today) {
        List<Batch> batches = new ArrayList<>();
        for (BatchEntity entity : jpa.findByProductIdAndQuantityRemainingGreaterThan(productId, 0)) {
            Batch batch = entity.toDomain();
            if (!batch.isExpired(today)) {
                batches.add(batch);
            }
        }
        batches.sort((a, b) -> FifoAllocator.compareFifo(a, b));
        return batches;
    }

    @Override
    public int availableStock(Long productId, LocalDate today) {
        int total = 0;
        for (Batch batch : findAvailableByProduct(productId, today)) {
            total += batch.getQuantityRemaining();
        }
        return total;
    }

    @Override
    public List<Batch> findExpiringBefore(LocalDate limit) {
        List<Batch> batches = new ArrayList<>();
        for (BatchEntity entity : jpa.findByExpirationDateBeforeAndQuantityRemainingGreaterThan(limit, 0)) {
            batches.add(entity.toDomain());
        }
        return batches;
    }

    @Override
    public Batch save(Batch batch) {
        return jpa.save(BatchEntity.fromDomain(batch)).toDomain();
    }

    @Override
    public Optional<Batch> findById(Long id) {
        return jpa.findById(id).map(BatchEntity::toDomain);
    }
}
