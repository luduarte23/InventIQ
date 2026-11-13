package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.PurchaseOrderLine;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Fila de la tabla 'purchase_order_lines': un producto de la orden.
 * Es el otro lado de @OneToMany: muchas líneas pertenecen a una orden (@ManyToOne).
 */
@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLineEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrderEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_ordered", nullable = false)
    private int quantityOrdered;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "added_by_moq", nullable = false)
    private boolean addedByMoq;

    /** JPA necesita un constructor vacío. */
    protected PurchaseOrderLineEntity() {
    }

    /** Del dominio a la tabla. Recibe la orden a la que pertenece la línea. */
    public static PurchaseOrderLineEntity fromDomain(PurchaseOrderLine line, PurchaseOrderEntity order) {
        PurchaseOrderLineEntity entity = new PurchaseOrderLineEntity();
        entity.id = line.getId();
        entity.order = order;
        entity.productId = line.getProductId();
        entity.quantityOrdered = line.getQuantityOrdered();
        entity.quantityReceived = line.getQuantityReceived();
        entity.unitCost = line.getUnitCost();
        entity.addedByMoq = line.isAddedByMoq();
        return entity;
    }

    /** De la tabla al dominio. */
    public PurchaseOrderLine toDomain() {
        return new PurchaseOrderLine(id, order.getId(), productId, quantityOrdered,
                quantityReceived, unitCost, addedByMoq);
    }
}
