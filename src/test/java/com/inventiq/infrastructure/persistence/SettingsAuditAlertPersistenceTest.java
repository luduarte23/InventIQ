package com.inventiq.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.model.Settings;
import com.inventiq.infrastructure.persistence.adapter.AlertRepositoryAdapter;
import com.inventiq.infrastructure.persistence.adapter.AuditLogRepositoryAdapter;
import com.inventiq.infrastructure.persistence.adapter.SettingsRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba los adaptadores de Settings, AuditLog y Alert (A5) contra H2 con las tablas
 * reales de Flyway, incluida la fila inicial de settings que crea la migración V2.
 */
@DataJpaTest
@Import({SettingsRepositoryAdapter.class, AuditLogRepositoryAdapter.class, AlertRepositoryAdapter.class})
class SettingsAuditAlertPersistenceTest {

    private static final Instant T1 = Instant.parse("2026-11-05T10:00:00Z");
    private static final Instant T2 = Instant.parse("2026-11-05T11:00:00Z");
    private static final Instant T3 = Instant.parse("2026-11-05T12:00:00Z");

    @Autowired private SettingsRepositoryAdapter settings;
    @Autowired private AuditLogRepositoryAdapter auditLogs;
    @Autowired private AlertRepositoryAdapter alerts;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    // --- Settings ---

    @Test
    void theInitialSettingsRowComesFromMigrationV2() {
        Settings current = settings.get();

        assertEquals(new BigDecimal("0.00"), current.getWeeklyBudget());
        assertEquals(30, current.getSalesWindowDays());
        assertEquals(7, current.getReviewPeriodDays());
        assertEquals(30, current.getExpiryReviewHorizonDays());
        assertEquals("+570000000000", current.getAdminWhatsappNumber());
    }

    @Test
    void changesAreSavedOnTheSameSingleRow() {
        Settings current = settings.get();
        current.changeWeeklyBudget(new BigDecimal("500000"));
        current.changeSalesWindowDays(60);
        current.changeAdminWhatsappNumber("+573001234567");

        settings.save(current);
        reloadFromDatabase();

        Settings read = settings.get();
        assertEquals(new BigDecimal("500000.00"), read.getWeeklyBudget());
        assertEquals(60, read.getSalesWindowDays());
        assertEquals("+573001234567", read.getAdminWhatsappNumber());
        assertEquals(1, jdbc.queryForObject("select count(*) from settings", Integer.class));
    }

    // --- AuditLog ---

    @Test
    void findRecentReturnsTheNewestEntriesFirst() {
        auditLogs.save(AuditLog.of(null, AuditAction.ROP_RECALCULATED, "Product", 1L, "ROP 12", T1));
        auditLogs.save(AuditLog.of(null, AuditAction.DRAFT_ORDER_CREATED, "PurchaseOrder", 5L, null, T2));
        auditLogs.save(AuditLog.of(null, AuditAction.DISCOUNT_APPLIED, "Batch", 8L, "15 %", T3));
        reloadFromDatabase();

        List<AuditLog> recent = auditLogs.findRecent(2);

        assertEquals(2, recent.size());
        assertEquals(AuditAction.DISCOUNT_APPLIED, recent.get(0).action());
        assertEquals(T3, recent.get(0).createdAt());
        assertEquals(AuditAction.DRAFT_ORDER_CREATED, recent.get(1).action());
    }

    @Test
    void anEntryKeepsItsActorAndDetails() {
        jdbc.update("insert into users (username, password_hash, role) values ('admin', 'x', 'ADMIN')");
        Long adminId = jdbc.queryForObject("select id from users", Long.class);

        AuditLog saved = auditLogs.save(AuditLog.of(adminId, AuditAction.ORDER_APPROVED,
                "PurchaseOrder", 5L, "Aprobada con 3 líneas", T1));
        reloadFromDatabase();

        AuditLog read = auditLogs.findRecent(1).get(0);
        assertNotNull(saved.id());
        assertEquals(adminId, read.actorUserId());
        assertEquals("PurchaseOrder", read.entityType());
        assertEquals(5L, read.entityId());
        assertEquals("Aprobada con 3 líneas", read.details());
    }

    @Test
    void findRecentWithZeroReturnsNothing() {
        auditLogs.save(AuditLog.of(null, AuditAction.ROP_RECALCULATED, "Product", 1L, null, T1));

        assertTrue(auditLogs.findRecent(0).isEmpty());
    }

    // --- Alert ---

    @Test
    void findUnresolvedSkipsResolvedAlerts() {
        Alert lowStock = alerts.save(Alert.raise(AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, T1));
        alerts.save(Alert.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Leche sin proveedor", "Product", 2L, T2));
        lowStock.resolve();
        alerts.save(lowStock);
        reloadFromDatabase();

        List<Alert> unresolved = alerts.findUnresolved();

        assertEquals(1, unresolved.size());
        assertEquals(AlertType.PRODUCT_WITHOUT_SUPPLIER, unresolved.get(0).getType());
        assertFalse(unresolved.get(0).isResolved());
    }

    @Test
    void anAlertKeepsItsRelatedEntityOrNone() {
        alerts.save(Alert.raise(AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, T1));
        alerts.save(Alert.raise(AlertType.NOTIFICATION_FAILED, "Twilio no respondió", null, null, T2));
        reloadFromDatabase();

        List<Alert> unresolved = alerts.findUnresolved();   // más reciente primero

        assertNull(unresolved.get(0).getRelatedEntityType());
        assertNull(unresolved.get(0).getRelatedEntityId());
        assertTrue(unresolved.get(1).isAbout("Product", 1L));
        assertEquals("Quedan 2 yogures", unresolved.get(1).getMessage());
    }

    /** Escribe en la base lo pendiente y olvida lo que JPA tenía en memoria, para leer de verdad de H2. */
    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}
