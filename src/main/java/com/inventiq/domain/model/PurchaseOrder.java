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

public class PurchaseOrder {

    private Long id;
    private Long supplierId;
    private OrderStatus status;
    private boolean needsReview;
    private boolean deferredByBudget;
    private Instant createdAt;
    private Instant approvedAt;
    private Instant sentAt;
    private Instant receivedAt;
    private final List<PurchaseOrderLine> lines;
    private BigDecimal totalCost;

    public PurchaseOrder(Long id, Long supplierId, OrderStatus status, boolean needsReview,
                         boolean deferredByBudget, Instant createdAt, Instant approvedAt,
                         Instant sentAt, Instant receivedAt, List<PurchaseOrderLine> lines) {
        this.id = id;
        this.supplierId = supplierId;
        this.status = Objects.requireNonNull(status, "status es obligatorio");
        this.needsReview = needsReview;
        this.deferredByBudget = deferredByBudget;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        this.approvedAt = approvedAt;
        this.sentAt = sentAt;
        this.receivedAt = receivedAt;
        this.lines = new ArrayList<>(lines == null ? List.of() : lines);
        recalculateTotal();
    }

    /** Crea una orden nueva en borrador, sin id ni líneas. */
    public static PurchaseOrder draft(Long supplierId, Instant now) {
        return new PurchaseOrder(null, supplierId, OrderStatus.DRAFT, false, false,
                now, null, null, null, List.of());
    }

    // --- Edición (solo en borrador) ---

    public void addLine(PurchaseOrderLine line) {
        requireStatus(OrderStatus.DRAFT);
        Objects.requireNonNull(line, "line es obligatoria");
        if (findLine(line.getProductId()) != null) {
            throw new IllegalStateException("La orden ya tiene una línea para el producto "
                    + line.getProductId());
        }
        lines.add(line);
        recalculateTotal();
    }

    public void markForReview() { this.needsReview = true; }
    public void clearReview()   { this.needsReview = false; }

    public void deferByBudget()       { this.deferredByBudget = true; }
    public void clearBudgetDeferral() { this.deferredByBudget = false; }

    // --- Transiciones de estado ---

    public void approve(Instant now) {
        requireStatus(OrderStatus.DRAFT);
        if (lines.isEmpty()) {
            throw new IllegalStateException("No se puede aprobar una orden sin líneas");
        }
        if (needsReview) {
            throw new IllegalStateException("La orden necesita revisión antes de aprobarse");
        }
        this.status = OrderStatus.APPROVED;
        this.approvedAt = Objects.requireNonNull(now);
    }

    public void markSent(Instant now) {
        requireStatus(OrderStatus.APPROVED);
        this.status = OrderStatus.SENT;
        this.sentAt = Objects.requireNonNull(now);
    }

    /** Registra lo que llegó de un producto. La orden pasa a parcial o completa. */
    public void registerReception(Long productId, int quantity, Instant now) {
        if (status != OrderStatus.SENT && status != OrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Solo se puede recibir una orden enviada; estado: "
                    + status);
        }
        PurchaseOrderLine line = findLine(productId);
        if (line == null) {
            throw new IllegalArgumentException("El producto " + productId
                    + " no está en la orden " + id);
        }
        line.registerReceived(quantity);

        if (lines.stream().allMatch(PurchaseOrderLine::isFullyReceived)) {
            this.status = OrderStatus.RECEIVED;
            this.receivedAt = Objects.requireNonNull(now);
        } else {
            this.status = OrderStatus.PARTIALLY_RECEIVED;
        }
    }

    public void cancel() {
        if (status != OrderStatus.DRAFT && status != OrderStatus.APPROVED) {
            throw new IllegalStateException("No se puede cancelar una orden en estado " + status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    // --- Apoyo ---

    private void requireStatus(OrderStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("Se esperaba estado " + expected
                    + " pero la orden está en " + status);
        }
    }

    private PurchaseOrderLine findLine(Long productId) {
        return lines.stream()
                .filter(l -> l.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private void recalculateTotal() {
        this.totalCost = lines.stream()
                .map(PurchaseOrderLine::lineCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public Long getSupplierId() { return supplierId; }
    public OrderStatus getStatus() { return status; }
    public boolean isNeedsReview() { return needsReview; }
    public boolean isDeferredByBudget() { return deferredByBudget; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getApprovedAt() { return approvedAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getReceivedAt() { return receivedAt; }
    public List<PurchaseOrderLine> getLines() { return Collections.unmodifiableList(lines); }
    public BigDecimal getTotalCost() { return totalCost; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PurchaseOrder other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
