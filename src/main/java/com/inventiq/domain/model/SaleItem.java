/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class SaleItem {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long id;
    private Long productId;
    private Long batchId;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;

    public SaleItem(Long id, Long productId, Long batchId, int quantity,
                    BigDecimal unitPrice, BigDecimal discountPercent) {
        this.id = id;
        this.productId = Objects.requireNonNull(productId, "productId es obligatorio");
        this.batchId = Objects.requireNonNull(batchId, "batchId es obligatorio");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity debe ser mayor que 0");
        }
        this.quantity = quantity;
        Objects.requireNonNull(unitPrice, "unitPrice es obligatorio");
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice no puede ser negativo");
        }
        this.unitPrice = unitPrice;
        this.discountPercent = validatePercent(
                discountPercent == null ? BigDecimal.ZERO : discountPercent);
    }

    /** precio × cantidad, menos el descuento, redondeado a 2 decimales. */
    public BigDecimal subtotal() {
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal factor = HUNDRED.subtract(discountPercent)
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return gross.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validatePercent(BigDecimal percent) {
        if (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("discountPercent debe estar entre 0 y 100");
        }
        return percent;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getBatchId() { return batchId; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getDiscountPercent() { return discountPercent; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SaleItem other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
