package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** Por ahora de solo lectura; D4 agrega el registro y la edición. */
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    private boolean perishable;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "safety_stock")
    private int safetyStock;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    private boolean active;

    @Version
    private long version;

    protected ProductEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getBarcode() {
        return barcode;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public boolean isPerishable() {
        return perishable;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public Integer getReorderPoint() {
        return reorderPoint;
    }

    public boolean isActive() {
        return active;
    }
}
