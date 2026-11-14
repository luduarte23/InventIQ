package com.inventiq.application.replenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.application.purchasing.DatabaseAlertService;
import com.inventiq.application.purchasing.DatabaseAuditRecorder;
import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.SaleItem;
import com.inventiq.domain.model.Settings;
import com.inventiq.domain.model.Supplier;
import com.inventiq.fakes.InMemoryAlertRepository;
import com.inventiq.fakes.InMemoryAuditLogRepository;
import com.inventiq.fakes.InMemoryBatchRepository;
import com.inventiq.fakes.InMemoryProductRepository;
import com.inventiq.fakes.InMemoryPurchaseOrderRepository;
import com.inventiq.fakes.InMemorySaleRepository;
import com.inventiq.fakes.InMemorySettingsRepository;
import com.inventiq.fakes.InMemorySupplierRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pruebas del motor de reabastecimiento con repositorios falsos: sin Spring ni base de datos. */
class ReplenishmentServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-14T15:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneId.of("America/Bogota"));

    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemorySaleRepository sales = new InMemorySaleRepository();
    private final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    private final InMemoryBatchRepository batches = new InMemoryBatchRepository();
    private final InMemoryPurchaseOrderRepository orders = new InMemoryPurchaseOrderRepository();
    private final InMemoryAuditLogRepository auditLogs = new InMemoryAuditLogRepository();
    private final InMemoryAlertRepository alerts = new InMemoryAlertRepository();
    // Presupuesto de 500.000, ventana de 30 días y revisión cada 7 (valores por defecto).
    private final InMemorySettingsRepository settings = new InMemorySettingsRepository(
            Settings.withDefaults(new BigDecimal("500000"), "+573001234567"));
    private final ReplenishmentServiceImpl service = new ReplenishmentServiceImpl(products, batches,
            sales, suppliers, orders, settings, new DatabaseAuditRecorder(auditLogs, CLOCK),
            new DatabaseAlertService(alerts, CLOCK), CLOCK);

    {
        // El proveedor 1 entrega en 3 días y el 2 en 2 días.
        suppliers.add(new Supplier(1L, "Alpina", "+573005550001", 3, null, null, true));
        suppliers.add(new Supplier(2L, "Postobón", "+573005550003", 2, null, null, true));
    }

    @Test
    void theReorderPointComesFromTheLastThirtyDaysOfSales() {
        // 2 por día durante 30 días, proveedor de 3 días, stock de seguridad 4: ROP 10.
        products.save(product(1L, 1L, 4));
        sell(1L, 2, 1, 30);

        assertEquals(List.of(1L), service.recalculateReorderPoints());
        assertEquals(10, products.findById(1L).get().getReorderPoint());
        assertEquals(AuditAction.ROP_RECALCULATED, auditLogs.all().get(0).action());
    }

    @Test
    void salesOlderThanTheWindowDoNotCount() {
        // Solo vendió hace 40 días: sin historial, el ROP es el stock de seguridad.
        products.save(product(1L, 1L, 4));
        sell(1L, 5, 40, 40);

        service.recalculateReorderPoints();

        assertEquals(4, products.findById(1L).get().getReorderPoint());
    }

    @Test
    void withoutSupplierTheLeadTimeIsZero() {
        products.save(product(2L, null, 3));
        sell(2L, 2, 1, 30);

        service.recalculateReorderPoints();

        assertEquals(3, products.findById(2L).get().getReorderPoint());
    }

    @Test
    void productsWhoseReorderPointDidNotChangeAreNotSavedAgain() {
        products.save(product(1L, 1L, 4));
        sell(1L, 2, 1, 30);
        service.recalculateReorderPoints();

        assertTrue(service.recalculateReorderPoints().isEmpty());
        assertEquals(1, auditLogs.all().size());
    }

    @Test
    void oneDraftPerSupplierWithTheProductsAtOrBelowTheirReorderPoint() {
        // 4 productos del proveedor 1 y 2 del 2 en su ROP o por debajo; uno más sí tiene stock.
        for (long id = 1; id <= 6; id++) {
            products.save(productWithStock(id, id <= 4 ? 1L : 2L, 10, 5));
        }
        products.save(productWithStock(7L, 1L, 10, 30));

        List<PurchaseOrder> drafts = service.generateDraftOrders();

        assertEquals(2, drafts.size());
        assertEquals(4, drafts.get(0).getLines().size());
        assertEquals(2, drafts.get(1).getLines().size());
        // Sin historial se pide lo que falta para el stock de seguridad: 4 - 5 da el mínimo, 1.
        // Dos productos de 1 unidad a 2.300 cada uno: 4.600.
        assertEquals(new BigDecimal("4600.00"), drafts.get(1).getTotalCost());
    }

    @Test
    void productsWithoutSupplierGoToAnOrderToReviewWithAnAlert() {
        products.save(productWithStock(1L, null, 10, 2));

        PurchaseOrder draft = service.generateDraftOrders().get(0);

        assertTrue(draft.isNeedsReview());
        assertEquals(AlertType.PRODUCT_WITHOUT_SUPPLIER, alerts.findUnresolved().get(0).getType());
    }

    @Test
    void runningItAgainDoesNotDuplicateTheOpenDrafts() {
        products.save(productWithStock(1L, 1L, 10, 5));
        service.generateDraftOrders();

        assertTrue(service.generateDraftOrders().isEmpty());
        assertEquals(1, orders.findByStatus(OrderStatus.DRAFT).size());
    }

    @Test
    void itSuggestsFastSellingProductsToReachTheSupplierMinimum() {
        // El proveedor 3 pide 500.000; el borrador va en 400.000 y el producto 2 vende 3 por día.
        suppliers.add(new Supplier(3L, "Nutresa", "+573005550002", 3, new BigDecimal("500000"), null, true));
        products.save(product(2L, 3L, 0));
        sell(2L, 3, 1, 30);
        PurchaseOrder draft = draft(3L, 1L, 160, "2500");

        List<OrderLineSuggestion> suggestions = service.suggestMoqCompletion(draft.getId());

        // Faltan 100.000 y el producto 2 cuesta 2.300: 44 unidades, tope de 21 (3 por día x 7).
        assertEquals(1, suggestions.size());
        assertEquals(21, suggestions.get(0).suggestedQuantity());
    }

    @Test
    void withoutBudgetToReachTheMinimumItSuggestsNothingAndRaisesAnAlert() {
        suppliers.add(new Supplier(3L, "Nutresa", "+573005550002", 3, new BigDecimal("500000"), null, true));
        products.save(product(2L, 3L, 0));
        sell(2L, 3, 1, 30);
        settings.save(Settings.withDefaults(new BigDecimal("400000"), "+573001234567"));
        PurchaseOrder draft = draft(3L, 1L, 160, "2500");

        assertTrue(service.suggestMoqCompletion(draft.getId()).isEmpty());
        assertEquals(AlertType.ORDER_BELOW_MOQ_NO_BUDGET, alerts.findUnresolved().get(0).getType());
    }

    @Test
    void underBudgetTheMostProfitableDraftGoesFirstAndTheOtherIsDeferred() {
        // Dos borradores de 230.000 y presupuesto de 300.000: solo cabe uno. Gana el del
        // producto 2, que se vende 5 por día con margen de 700; el producto 1 no se vende.
        products.save(product(1L, 1L, 0));
        products.save(product(2L, 2L, 0));
        sell(2L, 5, 1, 30);
        PurchaseOrder slow = draft(1L, 1L, 100, "2300");
        PurchaseOrder fast = draft(2L, 2L, 100, "2300");

        BudgetResult result = service.prioritizeUnderBudget(List.of(slow.getId(), fast.getId()),
                new BigDecimal("300000"));

        assertEquals(List.of(fast.getId()), result.fundedOrderIds());
        assertTrue(orders.findById(slow.getId()).get().isDeferredByBudget());
    }

    @Test
    void theOverviewShowsFirstTheProductsThatNeedAnOrder() {
        // El producto 7 tiene stock de sobra; el 8 está por debajo de su ROP y vende 2 por día.
        products.save(productWithStock(7L, 1L, 10, 30));
        products.save(productWithStock(8L, 1L, 10, 5));
        sell(8L, 2, 1, 30);

        List<ReorderStatus> rows = service.reorderOverview();

        ReorderStatus first = rows.get(0);
        assertEquals(8L, first.productId());
        assertTrue(first.needsReorder());
        assertEquals(new BigDecimal("2.0000"), first.unitsPerDay());
        // 2 por día x (3 + 7) días + 4 de seguridad - 5 que hay = 19.
        assertEquals(19, first.suggestedQuantity());
        assertEquals(0, rows.get(1).suggestedQuantity());
    }

    private PurchaseOrder draft(Long supplierId, Long productId, int quantity, String unitCost) {
        return orders.save(new PurchaseOrder(null, supplierId, OrderStatus.DRAFT, false, false, NOW,
                null, null, null, List.of(new PurchaseOrderLine(null, null, productId, quantity, 0,
                        new BigDecimal(unitCost), false))));
    }

    /** Producto con un ROP ya guardado y un lote con 'stock' unidades vendibles. */
    private Product productWithStock(Long id, Long supplierId, int reorderPoint, int stock) {
        batches.save(new Batch(id, id, stock, stock, null, NOW, new BigDecimal("2300"),
                null, null, 0L));
        return new Product(id, "100" + id, "Producto " + id, new BigDecimal("3000"),
                new BigDecimal("2300"), false, supplierId, 4, reorderPoint, true, 0L);
    }

    private static Product product(Long id, Long supplierId, int safetyStock) {
        return new Product(id, "100" + id, "Producto " + id, new BigDecimal("3000"),
                new BigDecimal("2300"), false, supplierId, safetyStock, 0, true, 0L);
    }

    /** Una venta cerrada por día, desde hace 'fromDaysAgo' hasta hace 'toDaysAgo' días. */
    private void sell(Long productId, int units, int fromDaysAgo, int toDaysAgo) {
        for (int daysAgo = fromDaysAgo; daysAgo <= toDaysAgo; daysAgo++) {
            Instant moment = NOW.minus(daysAgo, ChronoUnit.DAYS);
            SaleItem item = new SaleItem(null, null, productId, 1L, units, new BigDecimal("3000"), null);
            sales.save(new Sale(null, 1L, moment, moment.plusSeconds(300), List.of(item)));
        }
    }
}
