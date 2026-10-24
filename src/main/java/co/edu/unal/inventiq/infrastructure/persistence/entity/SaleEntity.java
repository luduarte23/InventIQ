package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "sale")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "sale_id", nullable = false)
    private List<SaleItemEntity> items = new ArrayList<>();

    protected SaleEntity() {
    }

    public SaleEntity(Long cashierId, LocalDateTime createdAt, BigDecimal total, List<SaleItemEntity> items) {
        this.cashierId = cashierId;
        this.createdAt = createdAt;
        this.total = total;
        this.items = new ArrayList<>(items);
    }

    public Long getId() {
        return id;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<SaleItemEntity> getItems() {
        return items;
    }
}
