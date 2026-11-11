package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.SaleItem;
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
 * Fila de la tabla 'sale_items': una línea de la venta, de un solo lote.
 * Es el otro lado de @OneToMany: muchos ítems pertenecen a una venta (@ManyToOne).
 */
@Entity
@Table(name = "sale_items")
public class SaleItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sale_id", nullable = false)
    private SaleEntity sale;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "discount_percent", nullable = false)
    private BigDecimal discountPercent;

    /** JPA necesita un constructor vacío. */
    protected SaleItemEntity() {
    }

    /** Del dominio a la tabla. Recibe la venta a la que pertenece el ítem. */
    public static SaleItemEntity fromDomain(SaleItem item, SaleEntity sale) {
        SaleItemEntity entity = new SaleItemEntity();
        entity.id = item.getId();
        entity.sale = sale;
        entity.productId = item.getProductId();
        entity.batchId = item.getBatchId();
        entity.quantity = item.getQuantity();
        entity.unitPrice = item.getUnitPrice();
        entity.discountPercent = item.getDiscountPercent();
        return entity;
    }

    /** De la tabla al dominio. */
    public SaleItem toDomain() {
        return new SaleItem(id, sale.getId(), productId, batchId, quantity, unitPrice, discountPercent);
    }
}
