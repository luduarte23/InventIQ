package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import java.util.List;

import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SaleItem;
import co.edu.unal.inventiq.infrastructure.persistence.entity.SaleEntity;
import co.edu.unal.inventiq.infrastructure.persistence.entity.SaleItemEntity;

public final class SaleMapper {

    private SaleMapper() {
    }

    public static SaleEntity toEntity(Sale sale, long cashierId) {
        List<SaleItemEntity> items = sale.items().stream()
                .map(item -> new SaleItemEntity(item.productId(), item.batchId(), item.quantity(), item.unitPrice()))
                .toList();
        return new SaleEntity(cashierId, sale.createdAt(), sale.total(), items);
    }

    public static Sale toDomain(SaleEntity entity, String cashierUsername) {
        List<SaleItem> items = entity.getItems().stream()
                .map(item -> new SaleItem(item.getProductId(), item.getBatchId(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new Sale(entity.getId(), cashierUsername, entity.getCreatedAt(), items);
    }
}
