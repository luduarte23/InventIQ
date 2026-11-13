package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Batch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Fila de la tabla 'batches' (lotes). @Version activa el bloqueo optimista: si dos cajeros
 * descuentan el mismo lote al tiempo, el segundo recibe un error en vez de pisar el stock.
 */
@Entity
@Table(name = "batches")
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "quantity_remaining", nullable = false)
    private int quantityRemaining;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "unit_cost", nullable = false)
    private BigDecimal unitCost;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent;

    @Column(name = "purchase_order_id")
    private Long purchaseOrderId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /** JPA necesita un constructor vacío. */
    protected BatchEntity() {
    }

    /** Del dominio a la tabla. */
    public static BatchEntity fromDomain(Batch batch) {
        BatchEntity entity = new BatchEntity();
        entity.id = batch.getId();
        entity.productId = batch.getProductId();
        entity.quantityReceived = batch.getQuantityReceived();
        entity.quantityRemaining = batch.getQuantityRemaining();
        entity.expirationDate = batch.getExpirationDate();
        entity.receivedAt = batch.getReceivedAt();
        entity.unitCost = batch.getUnitCost();
        entity.discountPercent = batch.getDiscountPercent();
        entity.purchaseOrderId = batch.getPurchaseOrderId();
        entity.version = batch.getVersion();
        return entity;
    }

    /** De la tabla al dominio. */
    public Batch toDomain() {
        return new Batch(id, productId, quantityReceived, quantityRemaining, expirationDate,
                receivedAt, unitCost, discountPercent, purchaseOrderId, version);
    }
}
