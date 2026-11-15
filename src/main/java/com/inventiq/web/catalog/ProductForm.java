package com.inventiq.web.catalog;

import java.math.BigDecimal;

import com.inventiq.application.catalog.ProductData;
import com.inventiq.domain.model.Product;

/** Lo que llena el formulario de producto. Spring copia cada campo del HTML con los setters. */
public class ProductForm {

    private String barcode;
    private String name;
    private BigDecimal salePrice;
    private BigDecimal unitCost;
    private boolean perishable;
    private Long supplierId;
    private int safetyStock;

    /** Formulario lleno con los datos actuales, para editar. */
    public static ProductForm from(Product product) {
        ProductForm form = new ProductForm();
        form.barcode = product.getBarcode();
        form.name = product.getName();
        form.salePrice = product.getSalePrice();
        form.unitCost = product.getUnitCost();
        form.perishable = product.isPerishable();
        form.supplierId = product.getSupplierId();
        form.safetyStock = product.getSafetyStock();
        return form;
    }

    public ProductData toData() {
        return new ProductData(trim(barcode), trim(name), salePrice, unitCost, perishable,
                supplierId, safetyStock);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public boolean isPerishable() { return perishable; }
    public void setPerishable(boolean perishable) { this.perishable = perishable; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public int getSafetyStock() { return safetyStock; }
    public void setSafetyStock(int safetyStock) { this.safetyStock = safetyStock; }
}