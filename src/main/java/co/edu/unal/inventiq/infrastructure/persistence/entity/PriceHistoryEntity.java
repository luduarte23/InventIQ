package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un registro del historial de precios. Nunca se modifica: los cambios agregan registros. */
@Entity
@Table(name = "price_history")
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected PriceHistoryEntity() {
    }

    public PriceHistoryEntity(Long productId, BigDecimal salePrice, BigDecimal cost, LocalDateTime changedAt) {
        this.productId = productId;
        this.salePrice = salePrice;
        this.cost = cost;
        this.changedAt = changedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
