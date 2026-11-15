package com.inventiq.application.purchasing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.domain.model.Settings;
import com.inventiq.domain.model.Supplier;
import com.inventiq.fakes.InMemoryAuditLogRepository;
import com.inventiq.fakes.InMemoryBatchRepository;
import com.inventiq.fakes.InMemoryProductRepository;
import com.inventiq.fakes.InMemoryPurchaseOrderRepository;
import com.inventiq.fakes.InMemorySettingsRepository;
import com.inventiq.fakes.InMemorySupplierRepository;
import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.port.MessageNotifier;
import com.inventiq.domain.port.NotificationResult;
import com.inventiq.fakes.InMemoryAlertRepository;
import java.util.ArrayList;

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
    private final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    private final InMemoryAlertRepository alertRepository = new InMemoryAlertRepository();
    /** Notificador falso: guarda lo que "envía" y responde lo que diga notifierWorks. */
    private final List<String> sentMessages = new ArrayList<>();
    private boolean notifierWorks = true;
    private final MessageNotifier notifier = (number, body) -> {
        sentMessages.add(number + " | " + body);
        return notifierWorks ? NotificationResult.ok("SM1") : NotificationResult.failed("Sin señal");
    };
    private final PurchasingServiceImpl service = new PurchasingServiceImpl(settings, batches,
            orders, products, suppliers, notifier, new DatabaseAlertService(alertRepository, CLOCK),
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

    // ---------- Cierre con pendientes (HU-05 esc. 2) ----------

    @Test
    void aPartiallyReceivedOrderIsClosedKeepingWhatArrived() {
        PurchaseOrder order = sentOrderOf50();
        service.confirmReceipt(order.getId(), List.of(new ReceiptLine(7L, 30, EXPIRES)), ADMIN);

        PurchaseOrder closed = service.closeOrder(order.getId(), ADMIN);

        assertEquals(OrderStatus.CLOSED, closed.getStatus());
        assertEquals(30, service.batchesOf(7L).get(0).getQuantityRemaining());
        assertTrue(service.ordersAwaitingReception().isEmpty());
        assertEquals("Pedido cerrado con 20 unidades pendientes que ya no se esperan",
                auditLogs.findRecent(1).get(0).details());
    }

    @Test
    void onlyAPartiallyReceivedOrderCanBeClosed() {
        PurchaseOrder order = sentOrderOf50();

        assertThrows(IllegalStateException.class, () -> service.closeOrder(order.getId(), ADMIN));
    }

    // ---------- Pedidos (D7) ----------

    /** Borrador del proveedor 1 (Alpina) con 10 unidades del producto 7 a $2100. */
    private PurchaseOrder draftOf10() {
        suppliers.add(new Supplier(1L, "Alpina", "+573005550001", 3, null, null, true));
        products.save(new Product(7L, "7702001000012", "Yogur de fresa", new BigDecimal("3200"),
                new BigDecimal("2100"), true, 1L, 0, 0, true, null));
        products.save(new Product(8L, "7702001000029", "Kumis", new BigDecimal("2800"),
                new BigDecimal("1800"), true, 1L, 0, 0, true, null));
        PurchaseOrder order = PurchaseOrder.draft(1L, NOW);
        order.addLine(new PurchaseOrderLine(null, null, 7L, 10, 0, new BigDecimal("2100"), false));
        return orders.save(order);
    }

    @Test
    void draftLinesCanBeChangedAddedAndRemoved() {
        PurchaseOrder order = draftOf10();

        service.updateOrderLines(order.getId(), List.of(new OrderLineEdit(7L, 12, false),
                new OrderLineEdit(8L, 5, true)));
        PurchaseOrder edited = service.getOrder(order.getId());
        assertEquals(2, edited.getLines().size());
        assertEquals(new BigDecimal("34200.00"), edited.getTotalCost());

        service.updateOrderLines(order.getId(), List.of(new OrderLineEdit(8L, 0, false)));
        assertEquals(1, service.getOrder(order.getId()).getLines().size());
    }

    @Test
    void onlyADraftCanBeEdited() {
        PurchaseOrder order = draftOf10();
        service.approveOrder(order.getId(), ADMIN);

        assertThrows(IllegalStateException.class, () -> service.updateOrderLines(order.getId(),
                List.of(new OrderLineEdit(7L, 12, false))));
    }

    @Test
    void approvingMovesTheDraftToApprovedAndIsAudited() {
        PurchaseOrder order = draftOf10();

        PurchaseOrder approved = service.approveOrder(order.getId(), ADMIN);

        assertEquals(OrderStatus.APPROVED, approved.getStatus());
        assertEquals(NOW, approved.getApprovedAt());
        AuditLog entry = onlyAuditEntry();
        assertEquals(AuditAction.ORDER_APPROVED, entry.action());
        assertEquals("1 productos por $ 21000.00", entry.details());
    }

    @Test
    void aDraftWithoutSupplierCannotBeApproved() {
        PurchaseOrder noSupplier = PurchaseOrder.draft(null, NOW);
        noSupplier.addLine(new PurchaseOrderLine(null, null, 7L, 10, 0, new BigDecimal("2100"), false));
        Long id = orders.save(noSupplier).getId();

        assertThrows(IllegalArgumentException.class, () -> service.approveOrder(id, ADMIN));
        assertEquals(OrderStatus.DRAFT, service.getOrder(id).getStatus());
    }

        @Test
    void theSupplierMessageListsEveryProductAndTheTotal() {
        PurchaseOrder order = draftOf10();

        SupplierMessage message = service.buildSupplierMessage(order.getId());

        assertEquals("Alpina", message.supplierName());
        assertEquals("Hola Alpina, este es el pedido #" + order.getId() + ":\n- 10 x Yogur de fresa\n"
                + "Total: $ 21000.00\nGracias.", message.body());
    }

    @Test
    void sendingMarksTheOrderSentAndMessagesTheSupplier() {
        PurchaseOrder order = draftOf10();
        service.approveOrder(order.getId(), ADMIN);

        PurchaseOrder sent = service.sendOrder(order.getId(), ADMIN);

        assertEquals(OrderStatus.SENT, sent.getStatus());
        assertEquals(1, sentMessages.size());
        assertTrue(sentMessages.get(0).startsWith("+573005550001 | Hola Alpina"));
        assertTrue(alertRepository.findUnresolved().isEmpty());
    }

    @Test
    void ifTheMessageFailsTheOrderIsStillSentAndAnAlertIsRaised() {
        PurchaseOrder order = draftOf10();
        service.approveOrder(order.getId(), ADMIN);
        notifierWorks = false;

        PurchaseOrder sent = service.sendOrder(order.getId(), ADMIN);

        assertEquals(OrderStatus.SENT, sent.getStatus());
        assertEquals(AlertType.NOTIFICATION_FAILED, alertRepository.findUnresolved().get(0).getType());
    }

    @Test
    void onlyAnApprovedOrderCanBeSent() {
        PurchaseOrder order = draftOf10();

        assertThrows(IllegalStateException.class, () -> service.sendOrder(order.getId(), ADMIN));
        assertTrue(sentMessages.isEmpty());
    }

    private AuditLog onlyAuditEntry() {
        List<AuditLog> entries = auditLogs.findRecent(10);
        assertEquals(1, entries.size());
        return entries.get(0);
    }
}