package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla 'purchase_orders'. Una orden tiene muchas líneas (@OneToMany): al guardar
 * la orden, JPA guarda también sus líneas, y borra las que se quitaron (orphanRemoval).
 * El estado se guarda como texto (VARCHAR), igual que en la migración V1.
 */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "needs_review", nullable = false)
    private boolean needsReview;

    @Column(name = "deferred_by_budget", nullable = false)
    private boolean deferredByBudget;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "total_cost", nullable = false)
    private BigDecimal totalCost;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLineEntity> lines = new ArrayList<>();

    /** JPA necesita un constructor vacío. */
    protected PurchaseOrderEntity() {
    }

    /** Del dominio a la tabla, con todas sus líneas. */
    public static PurchaseOrderEntity fromDomain(PurchaseOrder order) {
        PurchaseOrderEntity entity = new PurchaseOrderEntity();
        entity.id = order.getId();
        entity.supplierId = order.getSupplierId();
        entity.status = order.getStatus().name();
        entity.needsReview = order.isNeedsReview();
        entity.deferredByBudget = order.isDeferredByBudget();
        entity.createdAt = order.getCreatedAt();
        entity.approvedAt = order.getApprovedAt();
        entity.sentAt = order.getSentAt();
        entity.receivedAt = order.getReceivedAt();
        entity.totalCost = order.getTotalCost();
        for (PurchaseOrderLine line : order.getLines()) {
            entity.lines.add(PurchaseOrderLineEntity.fromDomain(line, entity));
        }
        return entity;
    }

    /** De la tabla al dominio, con todas sus líneas. El total lo recalcula el dominio. */
    public PurchaseOrder toDomain() {
        List<PurchaseOrderLine> domainLines = new ArrayList<>();
        for (PurchaseOrderLineEntity line : lines) {
            domainLines.add(line.toDomain());
        }
        return new PurchaseOrder(id, supplierId, OrderStatus.valueOf(status), needsReview,
                deferredByBudget, createdAt, approvedAt, sentAt, receivedAt, domainLines);
    }

    public Long getId() {
        return id;
    }
}