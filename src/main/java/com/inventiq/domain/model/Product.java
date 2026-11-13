/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Product {

    private Long id;
    private String barcode;
    private String name;
    private BigDecimal salePrice;
    private BigDecimal unitCost;
    private boolean perishable;
    private Long supplierId;      // puede ser null: producto sin proveedor asignado
    private int safetyStock;
    private int reorderPoint;
    private boolean active;
    private Long version;         // control de concurrencia; lo maneja persistencia

    public Product(Long id, String barcode, String name, BigDecimal salePrice,
                   BigDecimal unitCost, boolean perishable, Long supplierId,
                   int safetyStock, int reorderPoint, boolean active, Long version) {
        this.id = id;
        this.barcode = requireText(barcode, "barcode");
        this.name = requireText(name, "name");
        this.salePrice = requireNonNegative(salePrice, "salePrice");
        this.unitCost = requireNonNegative(unitCost, "unitCost");
        this.perishable = perishable;
        this.supplierId = supplierId;
        this.safetyStock = requireNonNegative(safetyStock, "safetyStock");
        this.reorderPoint = requireNonNegative(reorderPoint, "reorderPoint");
        this.active = active;
        this.version = version;
    }

    public void changePrices(BigDecimal newSalePrice, BigDecimal newUnitCost) {
        this.salePrice = requireNonNegative(newSalePrice, "salePrice");
        this.unitCost = requireNonNegative(newUnitCost, "unitCost");
    }

    public void rename(String newName) {
        this.name = requireText(newName, "name");
    }

    public void assignSupplier(Long newSupplierId) {
        this.supplierId = newSupplierId;
    }

    public boolean hasSupplier() {
        return supplierId != null;
    }

    public void changeSafetyStock(int newSafetyStock) {
        this.safetyStock = requireNonNegative(newSafetyStock, "safetyStock");
    }

    /** El cálculo del ROP lo hace la regla de C; aquí solo se guarda el resultado. */
    public void updateReorderPoint(int newReorderPoint) {
        this.reorderPoint = requireNonNegative(newReorderPoint, "reorderPoint");
    }

    public boolean needsReorder(int currentStock) {
        return currentStock <= reorderPoint;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio");
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " es obligatorio");
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
        return value;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
        return value;
    }

    public Long getId() { return id; }
    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public BigDecimal getSalePrice() { return salePrice; }
    public BigDecimal getUnitCost() { return unitCost; }
    public boolean isPerishable() { return perishable; }
    public Long getSupplierId() { return supplierId; }
    public int getSafetyStock() { return safetyStock; }
    public int getReorderPoint() { return reorderPoint; }
    public boolean isActive() { return active; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}