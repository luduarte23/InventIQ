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
    private Long saleId;                // null hasta que la venta se guarda
    private Long productId;
    private Long batchId;               // lote del que se descontó
    private int quantity;
    private BigDecimal unitPrice;       // precio cobrado, YA con descuento (§3)
    private BigDecimal discountPercent; // descuento del lote al momento de la venta

    public SaleItem(Long id, Long saleId, Long productId, Long batchId, int quantity,
                    BigDecimal unitPrice, BigDecimal discountPercent) {
        this.id = id;
        this.saleId = saleId;
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

    /** Fórmula de §3: salePrice * (1 - discountPercent/100), escala 2, HALF_UP. */
    public static BigDecimal priceAfterDiscount(BigDecimal salePrice, BigDecimal discountPercent) {
        BigDecimal factor = HUNDRED.subtract(validatePercent(discountPercent))
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return salePrice.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    /** quantity * unitPrice. El descuento ya está dentro de unitPrice. */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validatePercent(BigDecimal percent) {
        Objects.requireNonNull(percent, "discountPercent es obligatorio");
        if (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("discountPercent debe estar entre 0 y 100");
        }
        return percent;
    }

    public Long getId() { return id; }
    public Long getSaleId() { return saleId; }
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