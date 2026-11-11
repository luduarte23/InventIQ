package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.SaleItem;
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
 * Fila de la tabla 'sales'. Una venta tiene muchos ítems (@OneToMany): al guardar o
 * borrar la venta, JPA guarda o borra también sus ítems (cascade).
 */
@Entity
@Table(name = "sales")
public class SaleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cashier_id", nullable = false)
    private Long cashierId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItemEntity> items = new ArrayList<>();

    /** JPA necesita un constructor vacío. */
    protected SaleEntity() {
    }

    /** Del dominio a la tabla, con todos sus ítems. */
    public static SaleEntity fromDomain(Sale sale) {
        SaleEntity entity = new SaleEntity();
        entity.id = sale.getId();
        entity.cashierId = sale.getCashierId();
        entity.createdAt = sale.getCreatedAt();
        entity.closedAt = sale.getClosedAt();
        entity.total = sale.getTotal();
        for (SaleItem item : sale.getItems()) {
            entity.items.add(SaleItemEntity.fromDomain(item, entity));
        }
        return entity;
    }

    /** De la tabla al dominio, con todos sus ítems. */
    public Sale toDomain() {
        List<SaleItem> domainItems = new ArrayList<>();
        for (SaleItemEntity item : items) {
            domainItems.add(item.toDomain());
        }
        return new Sale(id, cashierId, createdAt, closedAt, domainItems);
    }

    public Long getId() {
        return id;
    }
}
