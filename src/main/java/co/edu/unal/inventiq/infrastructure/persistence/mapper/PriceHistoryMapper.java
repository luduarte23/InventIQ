package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.infrastructure.persistence.entity.PriceHistoryEntity;

public final class PriceHistoryMapper {

    private PriceHistoryMapper() {
    }

    public static PriceChange toDomain(PriceHistoryEntity entity) {
        return new PriceChange(entity.getId(), entity.getProductId(), entity.getSalePrice(), entity.getCost(),
                entity.getChangedAt());
    }

    public static PriceHistoryEntity toNewEntity(PriceChange change) {
        return new PriceHistoryEntity(change.productId(), change.salePrice(), change.cost(), change.changedAt());
    }
}
