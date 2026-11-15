package com.inventiq.application.purchasing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.model.Settings;
import com.inventiq.fakes.InMemoryAuditLogRepository;
import com.inventiq.fakes.InMemoryBatchRepository;
import com.inventiq.fakes.InMemorySettingsRepository;

/** Pruebas de PurchasingServiceImpl con repositorios falsos: sin Spring ni base de datos. */
class PurchasingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-15T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Bogota"));
    private static final Long ADMIN = 1L;

    private final InMemorySettingsRepository settings = new InMemorySettingsRepository(
            Settings.withDefaults(new BigDecimal("500000"), "+573001234567"));
    private final InMemoryBatchRepository batches = new InMemoryBatchRepository();
    private final InMemoryAuditLogRepository auditLogs = new InMemoryAuditLogRepository();
    private final PurchasingServiceImpl service = new PurchasingServiceImpl(settings, batches,
            new DatabaseAuditRecorder(auditLogs, CLOCK), CLOCK);

    // ---------- Parámetros (FR-08) ----------

    @Test
    void changedSettingsAreSavedAndAuditedWithBeforeAndAfter() {
        Settings edited = new Settings(new BigDecimal("600000"), 30, 14, 30, "+573001234567");

        Settings saved = service.updateSettings(edited, ADMIN);

        assertEquals(new BigDecimal("600000"), saved.getWeeklyBudget());
        assertEquals(14, settings.get().getReviewPeriodDays());
        AuditLog entry = onlyAuditEntry();
        assertEquals(AuditAction.SETTINGS_CHANGED, entry.action());
        assertEquals(ADMIN, entry.actorUserId());
        assertTrue(entry.details().contains("presupuesto semanal: 500000 -> 600000"));
        assertTrue(entry.details().contains("periodo de revisión (días): 7 -> 14"));
    }

    @Test
    void savingTheSameSettingsLeavesNoAuditEntry() {
        Settings same = new Settings(new BigDecimal("500000.00"), 30, 7, 30, "+573001234567");

        service.updateSettings(same, ADMIN);

        assertTrue(auditLogs.findRecent(10).isEmpty());
    }

    private AuditLog onlyAuditEntry() {
        List<AuditLog> entries = auditLogs.findRecent(10);
        assertEquals(1, entries.size());
        return entries.get(0);
    }
}