package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Los enums del dominio (tipo y unidad) se guardan como texto y los convierte ProductMapper:
 * así el esquema no depende de cómo Hibernate mapee los enums en cada base de datos.
 */
@Entity
@Table(name = "product")
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String barcode;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(length = 80)
    private String brand;

    @Column(name = "net_content", precision = 10, scale = 2)
    private BigDecimal netContent;

    @Column(name = "content_unit", length = 5)
    private String contentUnit;

    @Column(length = 500)
    private String description;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false)
    private boolean perishable;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Column(name = "reorder_point")
    private Integer reorderPoint;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "sale_enabled", nullable = false)
    private boolean saleEnabled;

    @Version
    private long version;

    protected ProductEntity() {
    }

    public ProductEntity(String barcode) {
        this.barcode = barcode;
    }

    /** Copia todo menos el id, el código (no se edita) y la versión (la maneja JPA). */
    public void apply(String name, String category, String brand, BigDecimal netContent, String contentUnit,
                      String description, BigDecimal salePrice, BigDecimal cost, boolean perishable, Long supplierId,
                      int safetyStock, Integer reorderPoint, boolean active, boolean saleEnabled) {
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.netContent = netContent;
        this.contentUnit = contentUnit;
        this.description = description;
        this.salePrice = salePrice;
        this.cost = cost;
        this.perishable = perishable;
        this.supplierId = supplierId;
        this.safetyStock = safetyStock;
        this.reorderPoint = reorderPoint;
        this.active = active;
        this.saleEnabled = saleEnabled;
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

    public String getCategory() {
        return category;
    }

    public String getBrand() {
        return brand;
    }

    public BigDecimal getNetContent() {
        return netContent;
    }

    public String getContentUnit() {
        return contentUnit;
    }

    public String getDescription() {
        return description;
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

    public boolean isSaleEnabled() {
        return saleEnabled;
    }

    public long getVersion() {
        return version;
    }
}
