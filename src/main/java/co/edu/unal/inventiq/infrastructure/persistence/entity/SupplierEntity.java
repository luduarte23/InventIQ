package co.edu.unal.inventiq.infrastructure.persistence.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Por ahora de solo lectura; D2 y D4 agregan la escritura. */
@Entity
@Table(name = "supplier")
public class SupplierEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String whatsapp;

    @Column(name = "lead_time_days", nullable = false)
    private int leadTimeDays;

    @Column(name = "min_order_type", length = 10)
    private String minOrderType;

    @Column(name = "min_order_value", precision = 12, scale = 2)
    private BigDecimal minOrderValue;

    @Column(nullable = false)
    private boolean active;

    protected SupplierEntity() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public int getLeadTimeDays() {
        return leadTimeDays;
    }

    public String getMinOrderType() {
        return minOrderType;
    }

    public BigDecimal getMinOrderValue() {
        return minOrderValue;
    }

    public boolean isActive() {
        return active;
    }
}
