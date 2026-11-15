package com.inventiq.application.purchasing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.model.Settings;
import com.inventiq.fakes.InMemoryAuditLogRepository;
import com.inventiq.fakes.InMemoryBatchRepository;
import com.inventiq.fakes.InMemorySettingsRepository;
import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.fakes.InMemoryProductRepository;
import com.inventiq.fakes.InMemoryPurchaseOrderRepository;

/** Pruebas de PurchasingServiceImpl con repositorios falsos: sin Spring ni base de datos. */
class PurchasingServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-15T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Bogota"));
    private static final Long ADMIN = 1L;

    private final InMemorySettingsRepository settings = new InMemorySettingsRepository(
            Settings.withDefaults(new BigDecimal("500000"), "+573001234567"));
    private final InMemoryBatchRepository batches = new InMemoryBatchRepository();
    private final InMemoryAuditLogRepository auditLogs = new InMemoryAuditLogRepository();
    private final InMemoryPurchaseOrderRepository orders = new InMemoryPurchaseOrderRepository();
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final PurchasingServiceImpl service = new PurchasingServiceImpl(settings, batches,
            orders, products, new DatabaseAuditRecorder(auditLogs, CLOCK), CLOCK);

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

    // ---------- Ajuste de stock (FR-08) ----------

    /** Lote 1 del producto 7: recibió 10, quedan 6, vence el 20/12/2026. */
    private Batch batch() {
        return batches.save(new Batch(null, 7L, 10, 6, LocalDate.of(2026, 12, 20), NOW,
                new BigDecimal("2100"), null, null, null));
    }

    @Test
    void loweringTheStockConsumesFromTheBatchAndIsAudited() {
        Batch batch = batch();

        service.adjustStock(7L, batch.getId(), 4, "Se dañaron 2", ADMIN);

        assertEquals(4, batch.getQuantityRemaining());
        AuditLog entry = onlyAuditEntry();
        assertEquals(AuditAction.STOCK_ADJUSTED, entry.action());
        assertEquals("Lote 1: 6 -> 4. Motivo: Se dañaron 2", entry.details());
    }

    @Test
    void raisingWithinWhatWasReceivedRestoresTheSameBatch() {
        Batch batch = batch();

        service.adjustStock(7L, batch.getId(), 10, "Conteo físico", ADMIN);

        assertEquals(10, batch.getQuantityRemaining());
        assertEquals(1, service.batchesOf(7L).size());
    }

    @Test
    void raisingAboveWhatWasReceivedPutsTheExtraInANewAdjustmentBatch() {
        Batch batch = batch();

        service.adjustStock(7L, batch.getId(), 13, "Conteo físico", ADMIN);

        List<Batch> all = service.batchesOf(7L);
        assertEquals(10, batch.getQuantityRemaining());
        assertEquals(2, all.size());
        Batch extra = all.stream().filter(b -> !b.getId().equals(batch.getId())).findFirst().get();
        assertEquals(3, extra.getQuantityRemaining());
        assertEquals(batch.getExpirationDate(), extra.getExpirationDate());
        assertEquals(batch.getUnitCost(), extra.getUnitCost());
        assertTrue(onlyAuditEntry().details().contains("3 en un lote nuevo de ajuste"));
    }

    @Test
    void anAdjustmentNeedsAReasonAValidQuantityAndABatchOfThatProduct() {
        Batch batch = batch();

        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(7L, batch.getId(), 4, " ", ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(7L, batch.getId(), -1, "x", ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(7L, 99L, 4, "x", ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.adjustStock(8L, batch.getId(), 4, "x", ADMIN));
        assertTrue(auditLogs.findRecent(10).isEmpty());
    }

    @Test
    void anAdjustmentToTheSameQuantityChangesNothing() {
        Batch batch = batch();

        service.adjustStock(7L, batch.getId(), 6, "Conteo físico", ADMIN);

        assertEquals(6, batch.getQuantityRemaining());
        assertTrue(auditLogs.findRecent(10).isEmpty());
    }

        // ---------- Recepción de pedidos (FR-10, HU-05) ----------

    private static final LocalDate EXPIRES = LocalDate.of(2026, 12, 20);

    /** Pedido enviado con 50 unidades del producto 7 (perecedero) a $2100 cada una. */
    private PurchaseOrder sentOrderOf50() {
        products.save(new Product(7L, "7702001000012", "Yogur de fresa", new BigDecimal("3200"),
                new BigDecimal("2100"), true, 1L, 0, 0, true, null));
        PurchaseOrder order = PurchaseOrder.draft(1L, NOW);
        order.addLine(new PurchaseOrderLine(null, null, 7L, 50, 0, new BigDecimal("2100"), false));
        order.approve(NOW);
        order.markSent(NOW);
        return orders.save(order);
    }

    @Test
    void hu05Scenario1EverythingArrivesInOneBatchAndTheOrderIsReceived() {
        PurchaseOrder order = sentOrderOf50();

        PurchaseOrder received = service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 50, EXPIRES)), ADMIN);

        assertEquals(OrderStatus.RECEIVED, received.getStatus());
        List<Batch> created = service.batchesOf(7L);
        assertEquals(1, created.size());
        assertEquals(50, created.get(0).getQuantityRemaining());
        assertEquals(EXPIRES, created.get(0).getExpirationDate());
        assertEquals(new BigDecimal("2100"), created.get(0).getUnitCost());
        assertEquals(order.getId(), created.get(0).getPurchaseOrderId());
        AuditLog entry = onlyAuditEntry();
        assertEquals(AuditAction.ORDER_RECEIVED, entry.action());
        assertEquals("50 unidades en 1 productos; estado RECEIVED; pendientes: 0", entry.details());
    }

    @Test
    void hu05Scenario2OnlyThirtyArriveAndTwentyStayPending() {
        PurchaseOrder order = sentOrderOf50();

        PurchaseOrder partial = service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 30, EXPIRES)), ADMIN);

        assertEquals(OrderStatus.PARTIALLY_RECEIVED, partial.getStatus());
        assertEquals(20, partial.getLines().get(0).pendingQuantity());
        assertEquals(30, service.batchesOf(7L).get(0).getQuantityRemaining());
        assertEquals(List.of(partial), service.ordersAwaitingReception());
    }

    @Test
    void theRestCanArriveLaterAndCompleteTheOrder() {
        PurchaseOrder order = sentOrderOf50();
        service.confirmReceipt(order.getId(), List.of(new ReceiptLine(7L, 30, EXPIRES)), ADMIN);

        PurchaseOrder done = service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 20, EXPIRES)), ADMIN);

        assertEquals(OrderStatus.RECEIVED, done.getStatus());
        assertEquals(2, service.batchesOf(7L).size());
        assertTrue(service.ordersAwaitingReception().isEmpty());
    }

    @Test
    void aPerishableWithoutDateOrWithAPastDateIsRejectedAndNothingChanges() {
        PurchaseOrder order = sentOrderOf50();

        assertThrows(IllegalArgumentException.class, () -> service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 30, null)), ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 30, LocalDate.of(2026, 11, 1))), ADMIN));

        assertEquals(OrderStatus.SENT, service.getOrder(order.getId()).getStatus());
        assertTrue(service.batchesOf(7L).isEmpty());
        assertTrue(auditLogs.findRecent(10).isEmpty());
    }

    @Test
    void moreThanPendingNothingOrAnotherProductIsRejected() {
        PurchaseOrder order = sentOrderOf50();

        assertThrows(IllegalArgumentException.class, () -> service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 51, EXPIRES)), ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(7L, 0, EXPIRES)), ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.confirmReceipt(order.getId(),
                List.of(new ReceiptLine(8L, 5, EXPIRES)), ADMIN));
        assertThrows(IllegalArgumentException.class, () -> service.getOrder(99L));
    }
    
    private AuditLog onlyAuditEntry() {
        List<AuditLog> entries = auditLogs.findRecent(10);
        assertEquals(1, entries.size());
        return entries.get(0);
    }
}