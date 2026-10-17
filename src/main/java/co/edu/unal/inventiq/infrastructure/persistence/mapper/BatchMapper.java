package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.infrastructure.persistence.entity.BatchEntity;

public final class BatchMapper {

    private BatchMapper() {
    }

    public static Batch toDomain(BatchEntity entity) {
        return new Batch(
                entity.getId(),
                entity.getProductId(),
                entity.getQuantityReceived(),
                entity.getQuantityAvailable(),
                entity.getExpirationDate(),
                entity.getReceivedAt(),
                entity.getVersion());
    }

    public static BatchEntity toNewEntity(Batch batch) {
        return new BatchEntity(
                batch.getProductId(),
                batch.getQuantityReceived(),
                batch.getQuantityAvailable(),
                batch.getExpirationDate(),
                batch.getReceivedAt(),
                null);
    }
}
