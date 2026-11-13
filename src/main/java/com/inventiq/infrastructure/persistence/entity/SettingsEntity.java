package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Settings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Fila de la tabla 'settings'. La tabla tiene una sola fila (CHECK id = 1), así que el id
 * no se genera: siempre es Settings.SINGLETON_ID. La fila inicial la crea la migración V2.
 */
@Entity
@Table(name = "settings")
public class SettingsEntity {

    @Id
    private Long id;

    @Column(name = "weekly_budget", nullable = false)
    private BigDecimal weeklyBudget;

    @Column(name = "sales_window_days", nullable = false)
    private int salesWindowDays;

    @Column(name = "review_period_days", nullable = false)
    private int reviewPeriodDays;

    @Column(name = "expiry_review_horizon_days", nullable = false)
    private int expiryReviewHorizonDays;

    @Column(name = "admin_whatsapp_number", nullable = false)
    private String adminWhatsappNumber;

    /** JPA necesita un constructor vacío. */
    protected SettingsEntity() {
    }

    /** Del dominio a la tabla. */
    public static SettingsEntity fromDomain(Settings settings) {
        SettingsEntity entity = new SettingsEntity();
        entity.id = Settings.SINGLETON_ID;
        entity.weeklyBudget = settings.getWeeklyBudget();
        entity.salesWindowDays = settings.getSalesWindowDays();
        entity.reviewPeriodDays = settings.getReviewPeriodDays();
        entity.expiryReviewHorizonDays = settings.getExpiryReviewHorizonDays();
        entity.adminWhatsappNumber = settings.getAdminWhatsappNumber();
        return entity;
    }

    /** De la tabla al dominio. */
    public Settings toDomain() {
        return new Settings(weeklyBudget, salesWindowDays, reviewPeriodDays,
                expiryReviewHorizonDays, adminWhatsappNumber);
    }
}