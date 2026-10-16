package co.edu.unal.inventiq.infrastructure.persistence.mapper;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NetContent;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.infrastructure.persistence.entity.ProductEntity;

public final class ProductMapper {

    private ProductMapper() {
    }

    public static Product toDomain(ProductEntity entity) {
        NetContent netContent = entity.getNetContent() == null || entity.getContentUnit() == null ? null
                : new NetContent(entity.getNetContent(), ContentUnit.valueOf(entity.getContentUnit()));
        return new Product(
                entity.getId(),
                entity.getBarcode(),
                entity.getName(),
                ProductCategory.valueOf(entity.getCategory()),
                entity.getBrand(),
                netContent,
                entity.getDescription(),
                entity.getSalePrice(),
                entity.getCost(),
                entity.isPerishable(),
                entity.getSupplierId(),
                entity.getSafetyStock(),
                entity.getReorderPoint(),
                entity.isActive(),
                entity.isSaleEnabled());
    }

    public static ProductEntity toNewEntity(Product product) {
        ProductEntity entity = new ProductEntity(product.barcode());
        copyInto(product, entity);
        return entity;
    }

    public static void copyInto(Product product, ProductEntity entity) {
        NetContent netContent = product.netContent();
        entity.apply(
                product.name(),
                product.category().name(),
                product.brand(),
                netContent == null ? null : netContent.amount(),
                netContent == null ? null : netContent.unit().name(),
                product.description(),
                product.salePrice(),
                product.cost(),
                product.perishable(),
                product.supplierId(),
                product.safetyStock(),
                product.reorderPoint(),
                product.active(),
                product.saleEnabled());
    }
}
