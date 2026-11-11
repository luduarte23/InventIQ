package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/**
 * Fila de la tabla 'products'. Es la versión JPA de Product: el dominio no lleva
 * anotaciones, esta clase sí. Sabe convertirse desde y hacia el Product del dominio.
 */
@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barcode", nullable = false, unique = true)
    private String barcode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sale_price", nullable = false)
    private BigDecimal salePrice;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "perishable", nullable = false)
    private boolean perishable;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Column(name = "reorder_point", nullable = false)
    private int reorderPoint;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** JPA necesita un constructor vacío. */
    protected ProductEntity() {
    }

    /** Del dominio a la tabla. */
    public static ProductEntity fromDomain(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.id = product.getId();
        entity.barcode = product.getBarcode();
        entity.name = product.getName();
        entity.salePrice = product.getSalePrice();
        entity.unitCost = product.getUnitCost();
        entity.perishable = product.isPerishable();
        entity.supplierId = product.getSupplierId();
        entity.safetyStock = product.getSafetyStock();
        entity.reorderPoint = product.getReorderPoint();
        entity.active = product.isActive();
        entity.version = product.getVersion();
        return entity;
    }

    /** De la tabla al dominio. */
    public Product toDomain() {
        return new Product(id, barcode, name, salePrice, unitCost, perishable, supplierId,
                safetyStock, reorderPoint, active, version);
    }
}
