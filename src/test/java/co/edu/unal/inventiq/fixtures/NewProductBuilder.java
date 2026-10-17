package co.edu.unal.inventiq.fixtures;

import java.math.BigDecimal;
import java.time.LocalDate;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NewProduct;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.ProductData;

/** Producto nuevo válido por defecto; cada prueba cambia solo lo que le interesa. */
public final class NewProductBuilder {

    private String barcode = "7702001000029";
    private String name = "Leche deslactosada";
    private ProductCategory category = ProductCategory.LACTEOS;
    private String brand = "La Pradera";
    private BigDecimal netContentAmount = BigDecimal.ONE;
    private ContentUnit contentUnit = ContentUnit.L;
    private String description = "Leche deslactosada en bolsa.";
    private BigDecimal salePrice = new BigDecimal("5200");
    private BigDecimal cost = new BigDecimal("3900");
    private boolean perishable = true;
    private Long supplierId;
    private Integer safetyStock = 6;
    private boolean saleEnabled = true;
    private Integer initialQuantity;
    private LocalDate initialExpiration;

    private NewProductBuilder() {
    }

    public static NewProductBuilder aValidProduct() {
        return new NewProductBuilder();
    }

    public NewProductBuilder barcode(String value) {
        this.barcode = value;
        return this;
    }

    public NewProductBuilder name(String value) {
        this.name = value;
        return this;
    }

    public NewProductBuilder category(ProductCategory value) {
        this.category = value;
        return this;
    }

    public NewProductBuilder brand(String value) {
        this.brand = value;
        return this;
    }

    public NewProductBuilder netContent(String amount, ContentUnit unit) {
        this.netContentAmount = amount == null ? null : new BigDecimal(amount);
        this.contentUnit = unit;
        return this;
    }

    public NewProductBuilder description(String value) {
        this.description = value;
        return this;
    }

    public NewProductBuilder salePrice(String value) {
        this.salePrice = value == null ? null : new BigDecimal(value);
        return this;
    }

    public NewProductBuilder cost(String value) {
        this.cost = value == null ? null : new BigDecimal(value);
        return this;
    }

    public NewProductBuilder perishable(boolean value) {
        this.perishable = value;
        return this;
    }

    public NewProductBuilder supplierId(Long value) {
        this.supplierId = value;
        return this;
    }

    public NewProductBuilder safetyStock(Integer value) {
        this.safetyStock = value;
        return this;
    }

    public NewProductBuilder saleEnabled(boolean value) {
        this.saleEnabled = value;
        return this;
    }

    public NewProductBuilder initialStock(Integer quantity, LocalDate expiration) {
        this.initialQuantity = quantity;
        this.initialExpiration = expiration;
        return this;
    }

    public ProductData buildData() {
        return new ProductData(name, category, brand, netContentAmount, contentUnit, description, salePrice, cost,
                perishable, supplierId, safetyStock);
    }

    public NewProduct build() {
        return new NewProduct(barcode, buildData(), saleEnabled, initialQuantity, initialExpiration);
    }
}
