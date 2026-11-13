package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Supplier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Fila de la tabla 'suppliers'. Los dos mínimos de pedido (MOQ) pueden ser null,
 * por eso son BigDecimal e Integer (objetos) y no int.
 */
@Entity
@Table(name = "suppliers")
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "whatsapp_number", nullable = false)
    private String whatsappNumber;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount;

    @Column(name = "min_order_units")
    private Integer minOrderUnits;

    @Column(name = "active", nullable = false)
    private boolean active;

    /** JPA necesita un constructor vacío. */
    protected SupplierEntity() {
    }

    /** Del dominio a la tabla. */
    public static SupplierEntity fromDomain(Supplier supplier) {
        SupplierEntity entity = new SupplierEntity();
        entity.id = supplier.getId();
        entity.name = supplier.getName();
        entity.whatsappNumber = supplier.getWhatsappNumber();
        entity.leadTimeDays = supplier.getLeadTimeDays();
        entity.minOrderAmount = supplier.getMinOrderAmount();
        entity.minOrderUnits = supplier.getMinOrderUnits();
        entity.active = supplier.isActive();
        return entity;
    }

    /** De la tabla al dominio. */
    public Supplier toDomain() {
        return new Supplier(id, name, whatsappNumber, leadTimeDays, minOrderAmount,
                minOrderUnits, active);
    }
}
