package co.edu.unal.inventiq.fixtures;

import java.math.BigDecimal;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NetContent;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;

/** Producto válido por defecto (leche entera de 1 L); cada prueba cambia solo lo que le interesa. */
public final class ProductBuilder {

    private Long id = 1L;
    private String barcode = "7702001000029";
    private String name = "Leche entera";
    private ProductCategory category = ProductCategory.LACTEOS;
    private String brand = "La Pradera";
    private NetContent netContent = new NetContent(BigDecimal.ONE, ContentUnit.L);
    private String description;
    private BigDecimal salePrice = new BigDecimal("4500");
    private BigDecimal cost = new BigDecimal("3300");
    private boolean perishable = true;
    private Long supplierId;
    private int safetyStock;
    private Integer reorderPoint;
    private boolean active = true;
    private boolean saleEnabled = true;

    private ProductBuilder() {
    }

    public static ProductBuilder aProduct() {
        return new ProductBuilder();
    }

    public ProductBuilder id(Long value) {
        this.id = value;
        return this;
    }

    public ProductBuilder barcode(String value) {
        this.barcode = value;
        return this;
    }

    public ProductBuilder name(String value) {
        this.name = value;
        return this;
    }

    public ProductBuilder category(ProductCategory value) {
        this.category = value;
        return this;
    }

    public ProductBuilder brand(String value) {
        this.brand = value;
        return this;
    }

    public ProductBuilder netContent(String amount, ContentUnit unit) {
        this.netContent = amount == null ? null : new NetContent(new BigDecimal(amount), unit);
        return this;
    }

    public ProductBuilder description(String value) {
        this.description = value;
        return this;
    }

    public ProductBuilder salePrice(int value) {
        this.salePrice = BigDecimal.valueOf(value);
        return this;
    }

    public ProductBuilder cost(int value) {
        this.cost = BigDecimal.valueOf(value);
        return this;
    }

    public ProductBuilder perishable(boolean value) {
        this.perishable = value;
        return this;
    }

    public ProductBuilder supplierId(Long value) {
        this.supplierId = value;
        return this;
    }

    public ProductBuilder safetyStock(int value) {
        this.safetyStock = value;
        return this;
    }

    public ProductBuilder reorderPoint(Integer value) {
        this.reorderPoint = value;
        return this;
    }

    public ProductBuilder active(boolean value) {
        this.active = value;
        return this;
    }

    public ProductBuilder saleEnabled(boolean value) {
        this.saleEnabled = value;
        return this;
    }

    public Product build() {
        return new Product(id, barcode, name, category, brand, netContent, description, salePrice, cost, perishable,
                supplierId, safetyStock, reorderPoint, active, saleEnabled);
    }
}
