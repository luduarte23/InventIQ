package com.inventiq.application.purchasing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.fakes.InMemoryAlertRepository;
import com.inventiq.fakes.InMemoryAuditLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/** Pruebas de la auditoría y las alertas con repositorios falsos, sin Spring ni base de datos. */
class DatabaseAuditAndAlertTest {

    private static final Instant NOW = Instant.parse("2026-11-14T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Bogota"));

    private final InMemoryAuditLogRepository auditLogs = new InMemoryAuditLogRepository();
    private final InMemoryAlertRepository alerts = new InMemoryAlertRepository();

    @Test
    void theAuditRecorderSavesWhoDidWhatAndWhen() {
        new DatabaseAuditRecorder(auditLogs, CLOCK).record(AuditAction.ROP_RECALCULATED,
                "Product", 7L, "ROP de 8 a 10", null);

        AuditLog entry = auditLogs.all().get(0);
        assertEquals(AuditAction.ROP_RECALCULATED, entry.action());
        assertEquals(7L, entry.entityId());
        assertEquals(NOW, entry.createdAt());
        // Sin usuario: la decisión la tomó el sistema.
        assertNull(entry.actorUserId());
    }

    @Test
    void anAlertIsSavedWithoutBeingResolved() {
        new DatabaseAlertService(alerts, CLOCK).raise(AlertType.PRODUCT_WITHOUT_SUPPLIER,
                "Arepas sin proveedor", "Product", 3L);

        Alert alert = alerts.findUnresolved().get(0);
        assertEquals(AlertType.PRODUCT_WITHOUT_SUPPLIER, alert.getType());
        assertEquals(NOW, alert.getCreatedAt());
    }

    @Test
    void theSameOpenAlertIsNotRepeated() {
        DatabaseAlertService service = new DatabaseAlertService(alerts, CLOCK);

        service.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Arepas sin proveedor", "Product", 3L);
        service.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Arepas sin proveedor", "Product", 3L);
        // Otro producto sí genera una alerta nueva.
        service.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Panela sin proveedor", "Product", 4L);

        assertEquals(2, alerts.all().size());
    }

    @Test
    void ifTheOldAlertWasResolvedANewOneIsCreated() {
        DatabaseAlertService service = new DatabaseAlertService(alerts, CLOCK);
        service.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Arepas sin proveedor", "Product", 3L);
        Alert first = alerts.all().get(0);
        first.resolve();
        alerts.save(first);

        service.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, "Arepas sin proveedor", "Product", 3L);

        assertEquals(1, alerts.findUnresolved().size());
        assertEquals(2, alerts.all().size());
    }
}
