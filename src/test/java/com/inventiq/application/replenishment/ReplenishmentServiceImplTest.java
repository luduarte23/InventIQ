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
    // Ventana de 30 días y revisión cada 7 (valores por defecto).
    private final ReplenishmentServiceImpl service = new ReplenishmentServiceImpl(products, batches,
            sales, suppliers, orders,
            new InMemorySettingsRepository(Settings.withDefaults(new BigDecimal("500000"), "+573001234567")),
            new DatabaseAuditRecorder(auditLogs, CLOCK), new DatabaseAlertService(alerts, CLOCK), CLOCK);

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
