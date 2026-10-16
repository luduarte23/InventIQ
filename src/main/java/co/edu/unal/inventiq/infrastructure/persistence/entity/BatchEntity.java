package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/** @Version activa el bloqueo optimista: dos ventas simultáneas sobre el mismo lote no se pisan. */
@Entity
@Table(name = "batch")
public class BatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_received", nullable = false)
    private int quantityReceived;

    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @Column(name = "order_line_id")
    private Long orderLineId;

    @Version
    private long version;

    protected BatchEntity() {
    }

    /** @param orderLineId null si el lote no viene de un pedido (p. ej. stock inicial) */
    public BatchEntity(Long productId, int quantityReceived, int quantityAvailable, LocalDate expirationDate,
                       LocalDateTime receivedAt, Long orderLineId) {
        this.productId = productId;
        this.quantityReceived = quantityReceived;
        this.quantityAvailable = quantityAvailable;
        this.expirationDate = expirationDate;
        this.receivedAt = receivedAt;
        this.orderLineId = orderLineId;
    }

    public void setQuantityAvailable(int quantityAvailable) {
        this.quantityAvailable = quantityAvailable;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public Long getOrderLineId() {
        return orderLineId;
    }

    public long getVersion() {
        return version;
    }
}
