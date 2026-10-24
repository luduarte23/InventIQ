package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.infrastructure.persistence.entity.BatchEntity;

public final class BatchMapper {

    private BatchMapper() {
    }

    public static Batch toDomain(BatchEntity e) {
        return new Batch(e.getId(), e.getProductId(), e.getQuantityReceived(), e.getQuantityAvailable(),
                e.getExpirationDate(), e.getReceivedAt(), e.getVersion());
    }
}
