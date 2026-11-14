package com.inventiq.application.replenishment;

import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
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
import com.inventiq.domain.rules.BudgetAllocation;
import com.inventiq.domain.rules.BudgetCandidate;
import com.inventiq.domain.rules.BudgetPrioritizer;
import com.inventiq.domain.rules.MoqCandidate;
import com.inventiq.domain.rules.MoqCompletion;
import com.inventiq.domain.rules.MoqCompletionResult;
import com.inventiq.domain.rules.ReorderPointCalculator;
import com.inventiq.domain.rules.SalesVelocityCalculator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

    /**
     * Agrupa por proveedor los productos con stock vendible en su ROP o por debajo, con la
     * cantidad sugerida. Los productos sin proveedor van a un pedido "por revisar" con una alerta.
     * No duplica: si el proveedor ya tiene un borrador abierto, le agrega lo que falte.
     */
    @Override
    @Transactional
    public List<PurchaseOrder> generateDraftOrders() {
        Settings config = settings.get();
        Map<Long, Supplier> supplierById = supplierById();
        LocalDate today = LocalDate.now(clock);
        Map<Long, PurchaseOrder> drafts = new HashMap<>();
        for (PurchaseOrder open : orders.findByStatus(OrderStatus.DRAFT)) {
            drafts.put(open.getSupplierId(), open);
        }
        Set<PurchaseOrder> changed = new LinkedHashSet<>();
        for (Product product : products.findAllActive()) {
            int stock = batches.availableStock(product.getId(), today);
            PurchaseOrder draft = drafts.computeIfAbsent(product.getSupplierId(),
                    supplierId -> PurchaseOrder.draft(supplierId, Instant.now(clock)));
            if (!product.needsReorder(stock) || draft.getLines().stream()
                    .anyMatch(line -> line.getProductId().equals(product.getId()))) {
                continue;
            }
            int quantity = ReorderPointCalculator.suggestedQuantity(velocityOf(product, config),
                    leadTimeOf(product, supplierById), config.getReviewPeriodDays(),
                    product.getSafetyStock(), stock);
            draft.addLine(new PurchaseOrderLine(null, draft.getId(), product.getId(), quantity, 0,
                    product.getUnitCost(), false));
            changed.add(draft);
            if (!product.hasSupplier()) {
                alerts.raise(AlertType.PRODUCT_WITHOUT_SUPPLIER, product.getName()
                        + " necesita pedido pero no tiene proveedor", "Product", product.getId());
            }
        }
        List<PurchaseOrder> saved = new ArrayList<>();
        for (PurchaseOrder draft : changed) {
            PurchaseOrder order = orders.save(draft);
            audit.record(AuditAction.DRAFT_ORDER_CREATED, "PurchaseOrder", order.getId(),
                    order.getLines().size() + " productos, total " + order.getTotalCost(), null);
            saved.add(order);
        }
        return saved;
    }

    /**
     * Sugiere productos del mismo proveedor para llegar a su pedido mínimo. Solo sugiere: para
     * aceptarlos, la pantalla edita las líneas del pedido. Si el presupuesto semanal no alcanza,
     * no sugiere nada y deja una alerta para que el administrador decida.
     */
    @Override
    @Transactional
    public List<OrderLineSuggestion> suggestMoqCompletion(Long orderId) {
        PurchaseOrder order = findOrder(orderId);
        Optional<Supplier> supplier = order.getSupplierId() == null ? Optional.empty()
                : suppliers.findById(order.getSupplierId());
        if (supplier.isEmpty()) {
            return List.of();
        }
        Settings config = settings.get();
        List<MoqCandidate> candidates = new ArrayList<>();
        Map<Long, BigDecimal> velocityById = new HashMap<>();
        for (Product product : products.findAllActive()) {
            Optional<BigDecimal> velocity = velocityOf(product, config);
            boolean inOrder = order.getLines().stream()
                    .anyMatch(line -> line.getProductId().equals(product.getId()));
            if (supplier.get().getId().equals(product.getSupplierId()) && !inOrder && velocity.isPresent()) {
                candidates.add(new MoqCandidate(product.getId(), velocity.get(), product.getUnitCost()));
                velocityById.put(product.getId(), velocity.get());
            }
        }
        int orderUnits = order.getLines().stream().mapToInt(PurchaseOrderLine::getQuantityOrdered).sum();
        MoqCompletionResult result = new MoqCompletion().complete(supplier.get(), order.getTotalCost(),
                orderUnits, candidates, config.getReviewPeriodDays(), config.getWeeklyBudget());
        if (result.blockedByBudget()) {
            alerts.raise(AlertType.ORDER_BELOW_MOQ_NO_BUDGET, "El pedido a " + supplier.get().getName()
                    + " no llega al mínimo y el presupuesto no alcanza", "PurchaseOrder", orderId);
        } else if (!result.suggestions().isEmpty()) {
            audit.record(AuditAction.MOQ_SUGGESTION_MADE, "PurchaseOrder", orderId,
                    result.suggestions().size() + " productos sugeridos por " + result.extraCost(), null);
        }
        return result.suggestions().stream().map(s -> new OrderLineSuggestion(s.productId(),
                s.quantity(), s.lineCost(), "Se vende " + velocityById.get(s.productId()).toPlainString()
                        + " por día; ayuda a llegar al pedido mínimo")).toList();
    }

    /**
     * Si los borradores cuestan más que el presupuesto, aprueba primero los que más ganancia
     * dejan por día y marca los demás como aplazados por presupuesto.
     */
    @Override
    @Transactional
    public BudgetResult prioritizeUnderBudget(List<Long> draftOrderIds, BigDecimal availableBudget) {
        Settings config = settings.get();
        List<BudgetCandidate> candidates = new ArrayList<>();
        for (Long id : draftOrderIds) {
            PurchaseOrder order = findOrder(id);
            BigDecimal dailyProfit = BigDecimal.ZERO;
            for (PurchaseOrderLine line : order.getLines()) {
                Product product = products.findById(line.getProductId()).orElseThrow();
                dailyProfit = dailyProfit.add(BudgetPrioritizer.dailyProfit(product.getSalePrice(),
                        line.getUnitCost(), velocityOf(product, config).orElse(BigDecimal.ZERO)));
            }
            candidates.add(new BudgetCandidate(id, order.getTotalCost(), dailyProfit));
        }
        BudgetAllocation allocation = new BudgetPrioritizer().prioritize(candidates, availableBudget);
        for (Long id : allocation.deferredOrderIds()) {
            PurchaseOrder order = findOrder(id);
            order.deferByBudget();
            orders.save(order);
            audit.record(AuditAction.ORDER_DEFERRED_BY_BUDGET, "PurchaseOrder", id,
                    "No cabe en el presupuesto de " + availableBudget, null);
        }
        for (Long id : allocation.fundedOrderIds()) {
            PurchaseOrder order = findOrder(id);
            order.clearBudgetDeferral();
            orders.save(order);
        }
        return new BudgetResult(allocation.fundedOrderIds(), allocation.deferredOrderIds(),
                allocation.totalFunded());
    }

    private PurchaseOrder findOrder(Long id) {
        return orders.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pedido " + id + " no existe"));
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
