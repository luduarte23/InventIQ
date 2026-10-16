package co.edu.unal.inventiq.web.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.NewProduct;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.ProductData;

/**
 * Formulario de producto, para registrar y para editar. Los nombres de los campos son los mismos
 * de ProductData y NewProduct, así los errores del dominio se marcan en el campo correspondiente.
 */
public class ProductForm {

    private String barcode;
    private String name;
    private ProductCategory category;
    private String brand;
    private BigDecimal netContentAmount;
    private ContentUnit contentUnit;
    private String description;
    private BigDecimal salePrice;
    private BigDecimal cost;
    private boolean perishable;
    private Long supplierId;
    private Integer safetyStock = 0;
    private boolean saleEnabled = true;
    private Integer initialQuantity;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate initialExpiration;

    /** El formulario de edición, con los datos actuales del producto. */
    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.barcode = product.barcode();
        form.name = product.name();
        form.category = product.category();
        form.brand = product.brand();
        if (product.netContent() != null) {
            form.netContentAmount = plain(product.netContent().amount());
            form.contentUnit = product.netContent().unit();
        }
        form.description = product.description();
        form.salePrice = plain(product.salePrice());
        form.cost = plain(product.cost());
        form.perishable = product.perishable();
        form.supplierId = product.supplierId();
        form.safetyStock = product.safetyStock();
        form.saleEnabled = product.saleEnabled();
        return form;
    }

    /** 5200.00 → 5200 y 1.50 → 1.5, sin notación científica (stripTrailingZeros daría 5.2E+3). */
    private static BigDecimal plain(BigDecimal value) {
        return new BigDecimal(value.stripTrailingZeros().toPlainString());
    }

    public ProductData toProductData() {
        return new ProductData(name, category, brand, netContentAmount, contentUnit, description, salePrice, cost,
                perishable, supplierId, safetyStock);
    }

    public NewProduct toNewProduct() {
        return new NewProduct(barcode, toProductData(), saleEnabled, initialQuantity, initialExpiration);
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public BigDecimal getNetContentAmount() {
        return netContentAmount;
    }

    public void setNetContentAmount(BigDecimal netContentAmount) {
        this.netContentAmount = netContentAmount;
    }

    public ContentUnit getContentUnit() {
        return contentUnit;
    }

    public void setContentUnit(ContentUnit contentUnit) {
        this.contentUnit = contentUnit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public boolean isPerishable() {
        return perishable;
    }

    public void setPerishable(boolean perishable) {
        this.perishable = perishable;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Integer getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(Integer safetyStock) {
        this.safetyStock = safetyStock;
    }

    public boolean isSaleEnabled() {
        return saleEnabled;
    }

    public void setSaleEnabled(boolean saleEnabled) {
        this.saleEnabled = saleEnabled;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public LocalDate getInitialExpiration() {
        return initialExpiration;
    }

    public void setInitialExpiration(LocalDate initialExpiration) {
        this.initialExpiration = initialExpiration;
    }
}
