package com.inventiq.application.replenishment;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Settings;
import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.AlertService;
import com.inventiq.domain.port.AuditRecorder;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.domain.port.SaleRepository;
import com.inventiq.domain.port.SettingsRepository;
import com.inventiq.domain.port.SupplierRepository;
import com.inventiq.domain.rules.ReorderPointCalculator;
import com.inventiq.domain.rules.SalesVelocityCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Motor de reabastecimiento. Lee ventas, stock, proveedores y configuración por los puertos,
 * le pide los cálculos a las reglas del dominio y guarda el resultado. Solo orquesta.
 */
@Service
public class ReplenishmentServiceImpl implements ReplenishmentService {

    private final ProductRepository products;
    private final BatchRepository batches;
    private final SaleRepository sales;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final SettingsRepository settings;
    private final AuditRecorder audit;
    private final AlertService alerts;
    private final Clock clock;

    public ReplenishmentServiceImpl(ProductRepository products, BatchRepository batches,
            SaleRepository sales, SupplierRepository suppliers, PurchaseOrderRepository orders,
            SettingsRepository settings, AuditRecorder audit, AlertService alerts, Clock clock) {
        this.products = products;
        this.batches = batches;
        this.sales = sales;
        this.suppliers = suppliers;
        this.orders = orders;
        this.settings = settings;
        this.audit = audit;
        this.alerts = alerts;
        this.clock = clock;
    }

    /** Recalcula el ROP de cada producto activo; guarda y audita solo los que cambiaron. */
    @Override
    @Transactional
    public List<Long> recalculateReorderPoints() {
        Settings config = settings.get();
        Map<Long, Supplier> supplierById = supplierById();
        List<Long> updated = new ArrayList<>();
        for (Product product : products.findAllActive()) {
            Optional<BigDecimal> velocity = velocityOf(product, config);
            int newRop = ReorderPointCalculator.reorderPoint(velocity,
                    leadTimeOf(product, supplierById), product.getSafetyStock());
            int oldRop = product.getReorderPoint();
            if (newRop != oldRop) {
                product.updateReorderPoint(newRop);
                products.save(product);
                audit.record(AuditAction.ROP_RECALCULATED, "Product", product.getId(), "ROP de "
                        + oldRop + " a " + newRop + "; velocidad diaria "
                        + velocity.map(BigDecimal::toPlainString).orElse("sin historial"), null);
                updated.add(product.getId());
            }
        }
        return updated;
    }

    @Override
    public List<PurchaseOrder> generateDraftOrders() {
        throw new UnsupportedOperationException("Todavía no implementado");
    }

    @Override
    public List<OrderLineSuggestion> suggestMoqCompletion(Long orderId) {
        throw new UnsupportedOperationException("Todavía no implementado");
    }

    @Override
    public BudgetResult prioritizeUnderBudget(List<Long> draftOrderIds, BigDecimal availableBudget) {
        throw new UnsupportedOperationException("Todavía no implementado");
    }

    /** Unidades por día en la ventana de ventas de la configuración (solo ventas cerradas). */
    private Optional<BigDecimal> velocityOf(Product product, Settings config) {
        int days = config.getSalesWindowDays();
        Instant since = Instant.now(clock).minus(days, ChronoUnit.DAYS);
        return SalesVelocityCalculator.unitsPerDay(sales.unitsSoldSince(product.getId(), since), days);
    }

    /** Días que tarda el proveedor; sin proveedor no se conoce el plazo y se toma 0. */
    private static int leadTimeOf(Product product, Map<Long, Supplier> supplierById) {
        Supplier supplier = product.hasSupplier() ? supplierById.get(product.getSupplierId()) : null;
        return supplier == null ? 0 : supplier.getLeadTimeDays();
    }

    private Map<Long, Supplier> supplierById() {
        return suppliers.findAllActive().stream().collect(Collectors.toMap(Supplier::getId, s -> s));
    }
}
