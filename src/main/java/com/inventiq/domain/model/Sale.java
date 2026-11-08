/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Sale {

    private Long id;
    private Long cashierId;
    private Instant createdAt;
    private Instant closedAt;            // null mientras la venta está abierta
    private final List<SaleItem> items;
    private BigDecimal total;

    public Sale(Long id, Long cashierId, Instant createdAt, Instant closedAt,
                List<SaleItem> items) {
        this.id = id;
        this.cashierId = Objects.requireNonNull(cashierId, "cashierId es obligatorio");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        this.closedAt = closedAt;
        this.items = new ArrayList<>(items == null ? List.of() : items);
        recalculateTotal();
    }

    /** Abre una venta nueva, todavía sin id ni ítems. */
    public static Sale open(Long cashierId, Instant now) {
        return new Sale(null, cashierId, now, null, List.of());
    }

    public void addItem(SaleItem item) {
        requireOpen();
        items.add(Objects.requireNonNull(item, "item es obligatorio"));
        recalculateTotal();
    }

    public void close(Instant now) {
        requireOpen();
        if (items.isEmpty()) {
            throw new IllegalStateException("No se puede cerrar una venta sin ítems");
        }
        this.closedAt = Objects.requireNonNull(now, "now es obligatorio");
    }

    public boolean isClosed() {
        return closedAt != null;
    }

    private void requireOpen() {
        if (isClosed()) {
            throw new IllegalStateException("La venta " + id + " ya está cerrada");
        }
    }

    private void recalculateTotal() {
        this.total = items.stream()
                .map(SaleItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public Long getCashierId() { return cashierId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getClosedAt() { return closedAt; }
    public List<SaleItem> getItems() { return Collections.unmodifiableList(items); }
    public BigDecimal getTotal() { return total; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Sale other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
