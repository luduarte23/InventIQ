package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.infrastructure.persistence.entity.ProductEntity;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toDomain(ProductEntity e) {
        return new Product(e.getId(), e.getBarcode(), e.getName(), e.getSalePrice(), e.getCost(), e.isPerishable(),
                e.getSupplierId(), e.getSafetyStock(), e.getReorderPoint(), e.isActive());
    }
}
