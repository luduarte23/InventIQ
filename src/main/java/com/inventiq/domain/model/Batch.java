/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Batch {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private Long id;
    private Long productId;
    private int quantityReceived;
    private int quantityRemaining;
    private LocalDate expirationDate;   // null si el producto no es perecedero
    private Instant receivedAt;
    private BigDecimal unitCost;
    private BigDecimal discountPercent;
    private Long purchaseOrderId;       // null si el lote no vino de una orden
    private Long version;

    public Batch(Long id, Long productId, int quantityReceived, int quantityRemaining,
                 LocalDate expirationDate, Instant receivedAt, BigDecimal unitCost,
                 BigDecimal discountPercent, Long purchaseOrderId, Long version) {
        this.id = id;
        this.productId = Objects.requireNonNull(productId, "productId es obligatorio");
        if (quantityReceived < 0) {
            throw new IllegalArgumentException("quantityReceived no puede ser negativo");
        }
        if (quantityRemaining < 0 || quantityRemaining > quantityReceived) {
            throw new IllegalArgumentException(
                    "quantityRemaining debe estar entre 0 y quantityReceived");
        }
        this.quantityReceived = quantityReceived;
        this.quantityRemaining = quantityRemaining;
        this.expirationDate = expirationDate;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt es obligatorio");
        this.unitCost = Objects.requireNonNull(unitCost, "unitCost es obligatorio");
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("unitCost no puede ser negativo");
        }
        this.discountPercent = validatePercent(
                discountPercent == null ? BigDecimal.ZERO : discountPercent);
        this.purchaseOrderId = purchaseOrderId;
        this.version = version;
    }

    /** Descuenta unidades de este lote. El orden FIFO entre lotes lo decide la regla de B. */
    public void consume(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity debe ser mayor que 0");
        }
        if (quantity > quantityRemaining) {
            throw new IllegalStateException("El lote " + id + " solo tiene "
                    + quantityRemaining + " unidades");
        }
        this.quantityRemaining -= quantity;
    }

    public boolean isDepleted() {
        return quantityRemaining == 0;
    }

    public boolean hasExpirationDate() {
        return expirationDate != null;
    }

    public boolean isExpired(LocalDate today) {
        return expirationDate != null && expirationDate.isBefore(today);
    }

    /** Qué porcentaje aplicar lo decide la regla de descuentos de C. */
    public void applyDiscount(BigDecimal percent) {
        this.discountPercent = validatePercent(percent);
    }

    private static BigDecimal validatePercent(BigDecimal percent) {
        Objects.requireNonNull(percent, "discountPercent es obligatorio");
        if (percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("discountPercent debe estar entre 0 y 100");
        }
        return percent;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public int getQuantityReceived() { return quantityReceived; }
    public int getQuantityRemaining() { return quantityRemaining; }
    public LocalDate getExpirationDate() { return expirationDate; }
    public Instant getReceivedAt() { return receivedAt; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getDiscountPercent() { return discountPercent; }
    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public Long getVersion() { return version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Batch other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}