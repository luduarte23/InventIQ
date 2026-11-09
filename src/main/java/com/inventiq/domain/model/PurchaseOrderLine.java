package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class PurchaseOrderLine {

    private Long id;
    private Long orderId;          // null hasta que la orden se guarda
    private Long productId;
    private int quantityOrdered;
    private int quantityReceived;
    private BigDecimal unitCost;
    private boolean addedByMoq;

    public PurchaseOrderLine(Long id, Long orderId, Long productId, int quantityOrdered,
                             int quantityReceived, BigDecimal unitCost, boolean addedByMoq) {
        this.id = id;
        this.orderId = orderId;
        this.productId = Objects.requireNonNull(productId, "productId es obligatorio");
        if (quantityOrdered <= 0) {
            throw new IllegalArgumentException("quantityOrdered debe ser mayor que 0");
        }
        if (quantityReceived < 0 || quantityReceived > quantityOrdered) {
            throw new IllegalArgumentException(
                    "quantityReceived debe estar entre 0 y quantityOrdered");
        }
        this.quantityOrdered = quantityOrdered;
        this.quantityReceived = quantityReceived;
        Objects.requireNonNull(unitCost, "unitCost es obligatorio");
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("unitCost no puede ser negativo");
        }
        this.unitCost = unitCost;
        this.addedByMoq = addedByMoq;
    }

    public BigDecimal lineCost() {
        return unitCost.multiply(BigDecimal.valueOf(quantityOrdered))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public int pendingQuantity() {
        return quantityOrdered - quantityReceived;
    }

    public boolean isFullyReceived() {
        return quantityReceived == quantityOrdered;
    }

    // Sin "public": solo PurchaseOrder (mismo paquete) puede modificar la línea.
    void edit(int newQuantityOrdered, boolean newAddedByMoq) {
        if (newQuantityOrdered <= 0) {
            throw new IllegalArgumentException("quantityOrdered debe ser mayor que 0");
        }
        this.quantityOrdered = newQuantityOrdered;
        this.addedByMoq = newAddedByMoq;
    }

    void registerReceived(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity debe ser mayor que 0");
        }
        if (quantity > pendingQuantity()) {
            throw new IllegalArgumentException("Se recibirían más unidades de las pedidas"
                    + " para el producto " + productId);
        }
        this.quantityReceived += quantity;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public int getQuantityOrdered() { return quantityOrdered; }
    public int getQuantityReceived() { return quantityReceived; }
    public BigDecimal getUnitCost() { return unitCost; }
    public boolean isAddedByMoq() { return addedByMoq; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrderLine other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}