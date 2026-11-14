package com.inventiq.application.replenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.application.purchasing.DatabaseAlertService;
import com.inventiq.application.purchasing.DatabaseAuditRecorder;
import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.Product;
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
    private final InMemoryAuditLogRepository auditLogs = new InMemoryAuditLogRepository();
    // Ventana de 30 días y revisión cada 7 (valores por defecto); el proveedor 1 entrega en 3 días.
    private final ReplenishmentServiceImpl service = new ReplenishmentServiceImpl(products,
            new InMemoryBatchRepository(), sales, suppliers, new InMemoryPurchaseOrderRepository(),
            new InMemorySettingsRepository(Settings.withDefaults(new BigDecimal("500000"), "+573001234567")),
            new DatabaseAuditRecorder(auditLogs, CLOCK),
            new DatabaseAlertService(new InMemoryAlertRepository(), CLOCK), CLOCK);

    {
        suppliers.add(new Supplier(1L, "Alpina", "+573005550001", 3, null, null, true));
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
