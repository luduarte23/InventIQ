package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.infrastructure.persistence.entity.BatchEntity;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.BatchJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.BatchMapper;

@Component
public class BatchRepositoryAdapter implements BatchRepository {

    private final BatchJpaRepository jpa;

    public BatchRepositoryAdapter(BatchJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Batch> findAvailableByProduct(long productId) {
        return jpa.findAvailableByProduct(productId).stream().map(BatchMapper::toDomain).toList();
    }

    @Override
    public List<Batch> findAllAvailable() {
        return jpa.findByQuantityAvailableGreaterThan(0).stream().map(BatchMapper::toDomain).toList();
    }

    @Override
    public Batch create(Batch batch) {
        return BatchMapper.toDomain(jpa.saveAndFlush(BatchMapper.toNewEntity(batch)));
    }

    /**
     * Solo cambia la cantidad disponible. Compara la versión que leyó el dominio con la
     * actual y, al hacer flush, el UPDATE ... WHERE version = ? detecta si otra transacción
     * se adelantó. Ambos casos se reportan como ConcurrentStockUpdateException.
     */
    @Override
    public void saveAll(List<Batch> batches) {
        Map<Long, BatchEntity> entities = jpa.findAllById(batches.stream().map(Batch::getId).toList()).stream()
                .collect(Collectors.toMap(BatchEntity::getId, Function.identity()));
        for (Batch batch : batches) {
            BatchEntity entity = entities.get(batch.getId());
            if (entity == null || entity.getVersion() != batch.getVersion()) {
                throw new ConcurrentStockUpdateException(null);
            }
            entity.setQuantityAvailable(batch.getQuantityAvailable());
        }
        try {
            jpa.saveAllAndFlush(entities.values());
        } catch (ConcurrencyFailureException e) {
            throw new ConcurrentStockUpdateException(e);
        }
    }
}
