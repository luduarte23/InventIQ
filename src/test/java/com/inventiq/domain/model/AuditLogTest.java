package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pruebas de AuditLog (record): creación, validaciones y recorte de details. */
class AuditLogTest {

    private static final Instant NOW = Instant.parse("2026-11-05T11:00:00Z");

    @Test
    void ofCreatesARecordWithoutId() {
        AuditLog log = AuditLog.of(2L, AuditAction.ORDER_APPROVED, "PurchaseOrder", 7L,
                "Aprobada por el administrador", NOW);

        assertNull(log.id());
        assertEquals(NOW, log.createdAt());
        assertEquals(2L, log.actorUserId());
        assertEquals(AuditAction.ORDER_APPROVED, log.action());
        assertEquals("PurchaseOrder", log.entityType());
        assertEquals(7L, log.entityId());
        assertEquals("Aprobada por el administrador", log.details());
    }

    @Test
    void theSchedulerActsWithoutUser() {
        AuditLog log = AuditLog.of(null, AuditAction.ROP_RECALCULATED, "Product", 1L, null, NOW);

        assertNull(log.actorUserId());
        assertNull(log.details());
    }

    @Test
    void requiredFieldsAreValidated() {
        assertThrows(NullPointerException.class,
                () -> AuditLog.of(null, AuditAction.ORDER_APPROVED, "PurchaseOrder", 7L, null, null));
        assertThrows(NullPointerException.class,
                () -> AuditLog.of(null, null, "PurchaseOrder", 7L, null, NOW));
        assertThrows(NullPointerException.class,
                () -> AuditLog.of(null, AuditAction.ORDER_APPROVED, "PurchaseOrder", null, null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> AuditLog.of(null, AuditAction.ORDER_APPROVED, " ", 7L, null, NOW));
    }

    @Test
    void detailsLongerThan2000CharactersAreCut() {
        AuditLog log = AuditLog.of(null, AuditAction.ROP_RECALCULATED, "Product", 1L,
                "x".repeat(2500), NOW);

        assertEquals(2000, log.details().length());
    }

    @Test
    void recordsWithTheSameValuesAreEqual() {
        AuditLog a = AuditLog.of(2L, AuditAction.STOCK_ADJUSTED, "Batch", 8L, "ajuste", NOW);
        AuditLog b = AuditLog.of(2L, AuditAction.STOCK_ADJUSTED, "Batch", 8L, "ajuste", NOW);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}