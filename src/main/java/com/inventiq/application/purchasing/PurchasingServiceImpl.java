package com.inventiq.application.purchasing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.domain.model.Settings;
import com.inventiq.domain.port.AuditRecorder;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.domain.port.SettingsRepository;


/**
 * Compras y administración (parte D). Lee y guarda por los puertos, deja las reglas al dominio
 * (Settings, Batch, PurchaseOrder) y audita cada cambio administrativo. Solo orquesta.
 */
@Service
public class PurchasingServiceImpl implements PurchasingService {

    private final SettingsRepository settings;
    private final BatchRepository batches;
    private final PurchaseOrderRepository orders;
    private final ProductRepository products;
    private final AuditRecorder audit;
    private final Clock clock;

    public PurchasingServiceImpl(SettingsRepository settings, BatchRepository batches,
                                 PurchaseOrderRepository orders, ProductRepository products,
                                 AuditRecorder audit, Clock clock) {
        this.settings = settings;
        this.batches = batches;
        this.orders = orders;
        this.products = products;
        this.audit = audit;
        this.clock = clock;
    }

    // ---------- FR-08: parámetros ----------

    @Override
    @Transactional(readOnly = true)
    public Settings getSettings() {
        return settings.get();
    }

    /**
     * Copia los valores nuevos sobre la fila única de configuración. Solo guarda y audita si algo
     * cambió, y deja en la auditoría cada cambio como "antes -> después".
     */
    @Override
    @Transactional
    public Settings updateSettings(Settings newSettings, Long adminUserId) {
        Settings current = settings.get();
        List<String> changes = new ArrayList<>();
        if (current.getWeeklyBudget().compareTo(newSettings.getWeeklyBudget()) != 0) {
            changes.add(change("presupuesto semanal", current.getWeeklyBudget(), newSettings.getWeeklyBudget()));
            current.changeWeeklyBudget(newSettings.getWeeklyBudget());
        }
        if (current.getSalesWindowDays() != newSettings.getSalesWindowDays()) {
            changes.add(change("ventana de ventas (días)", current.getSalesWindowDays(), newSettings.getSalesWindowDays()));
            current.changeSalesWindowDays(newSettings.getSalesWindowDays());
        }
        if (current.getReviewPeriodDays() != newSettings.getReviewPeriodDays()) {
            changes.add(change("periodo de revisión (días)", current.getReviewPeriodDays(), newSettings.getReviewPeriodDays()));
            current.changeReviewPeriodDays(newSettings.getReviewPeriodDays());
        }
        if (current.getExpiryReviewHorizonDays() != newSettings.getExpiryReviewHorizonDays()) {
            changes.add(change("horizonte de vencimientos (días)", current.getExpiryReviewHorizonDays(),
                    newSettings.getExpiryReviewHorizonDays()));
            current.changeExpiryReviewHorizonDays(newSettings.getExpiryReviewHorizonDays());
        }
        if (!current.getAdminWhatsappNumber().equals(newSettings.getAdminWhatsappNumber())) {
            changes.add(change("WhatsApp del administrador", current.getAdminWhatsappNumber(),
                    newSettings.getAdminWhatsappNumber()));
            current.changeAdminWhatsappNumber(newSettings.getAdminWhatsappNumber());
        }
        if (changes.isEmpty()) {
            return current;
        }
        Settings saved = settings.save(current);
        audit.record(AuditAction.SETTINGS_CHANGED, "Settings", Settings.SINGLETON_ID,
                String.join("; ", changes), adminUserId);
        return saved;
    }

    private static String change(String field, Object before, Object after) {
        return field + ": " + before + " -> " + after;
    }

        // ---------- FR-08: ajuste manual de stock ----------

    @Override
    @Transactional(readOnly = true)
    public List<Batch> batchesOf(Long productId) {
        return batches.findAvailableByProduct(productId, LocalDate.now(clock));
    }

    @Override
    @Transactional
    public void adjustStock(Long productId, Long batchId, int newQuantity, String reason,
                            Long adminUserId) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("El motivo del ajuste es obligatorio");
        }
        if (newQuantity < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa");
        }
        Batch batch = batches.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("El lote " + batchId + " no existe"));
        if (!Objects.equals(batch.getProductId(), productId)) {
            throw new IllegalArgumentException("El lote " + batchId + " no es del producto " + productId);
        }
        int before = batch.getQuantityRemaining();
        if (newQuantity == before) {
            return;
        }
        String details = "Lote " + batchId + ": " + before + " -> " + newQuantity;
        if (newQuantity < before) {
            batch.consume(before - newQuantity);
            batches.save(batch);
        } else {
            int room = batch.getQuantityReceived() - before;
            int restored = Math.min(room, newQuantity - before);
            if (restored > 0) {
                batch.restore(restored);
                batches.save(batch);
            }
            int extra = newQuantity - before - restored;
            if (extra > 0) {
                batches.save(new Batch(null, productId, extra, extra, batch.getExpirationDate(),
                        Instant.now(clock), batch.getUnitCost(), null, null, null));
                details += " (" + extra + " en un lote nuevo de ajuste)";
            }
        }
        audit.record(AuditAction.STOCK_ADJUSTED, "Batch", batchId,
                details + ". Motivo: " + reason.trim(), adminUserId);
    }

        // ---------- FR-10: recepción de pedidos (D6) ----------

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrder> ordersAwaitingReception() {
        List<PurchaseOrder> result = new ArrayList<>(orders.findByStatus(OrderStatus.SENT));
        result.addAll(orders.findByStatus(OrderStatus.PARTIALLY_RECEIVED));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrder getOrder(Long orderId) {
        return orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido " + orderId + " no existe"));
    }

    /**
     * HU-05: por cada línea que llegó crea un lote (con el costo de la línea y el id del pedido),
     * registra la recepción en el pedido y audita ORDER_RECEIVED. Es una sola transacción: si una
     * línea es inválida no se guarda ningún lote ni cambia el pedido.
     */
    @Override
    @Transactional
    public PurchaseOrder confirmReceipt(Long orderId, List<ReceiptLine> lines, Long adminUserId) {
        PurchaseOrder order = getOrder(orderId);
        List<ReceiptLine> arrived = lines.stream().filter(l -> l.quantityReceived() != 0).toList();
        if (arrived.isEmpty()) {
            throw new IllegalArgumentException("No se registró ninguna unidad recibida");
        }
        Instant now = Instant.now(clock);
        LocalDate today = LocalDate.now(clock);
        // 1) Se revisan todas las líneas antes de cambiar nada.
        for (ReceiptLine line : arrived) {
            validate(order, line, today);
        }
        // 2) Todas son válidas: se registra la recepción y se arma un lote por línea.
        List<Batch> newBatches = new ArrayList<>();
        int units = 0;
        for (ReceiptLine line : arrived) {
            order.registerReception(line.productId(), line.quantityReceived(), now);
            newBatches.add(new Batch(null, line.productId(), line.quantityReceived(),
                    line.quantityReceived(), line.expirationDate(), now,
                    lineOf(order, line.productId()).getUnitCost(), null, orderId, null));
            units += line.quantityReceived();
        }
        newBatches.forEach(batches::save);
        PurchaseOrder saved = orders.save(order);
        audit.record(AuditAction.ORDER_RECEIVED, "PurchaseOrder", orderId,
                units + " unidades en " + arrived.size() + " productos; estado " + saved.getStatus()
                        + "; pendientes: " + pendingUnits(saved), adminUserId);
        return saved;
    }

    /** HU-05 esc. 2. No hay acción ORDER_CLOSED: se audita como ORDER_RECEIVED con el detalle. */
    @Override
    @Transactional
    public PurchaseOrder closeOrder(Long orderId, Long adminUserId) {
        PurchaseOrder order = getOrder(orderId);
        int pending = pendingUnits(order);
        order.closeWithPending(Instant.now(clock));
        PurchaseOrder saved = orders.save(order);
        audit.record(AuditAction.ORDER_RECEIVED, "PurchaseOrder", orderId,
                "Pedido cerrado con " + pending + " unidades pendientes que ya no se esperan",
                adminUserId);
        return saved;
    }

    /** Una línea recibida es válida si el producto está en el pedido, la cantidad cabe en lo
     *  pendiente y la fecha de vencimiento existe (si es perecedero) y no ha pasado. */
    private void validate(PurchaseOrder order, ReceiptLine line, LocalDate today) {
        PurchaseOrderLine orderLine = lineOf(order, line.productId());
        Product product = products.findById(line.productId())
                .orElseThrow(() -> new IllegalArgumentException("El producto " + line.productId() + " no existe"));
        if (line.quantityReceived() < 0 || line.quantityReceived() > orderLine.pendingQuantity()) {
            throw new IllegalArgumentException(product.getName() + ": se pueden recibir entre 0 y "
                    + orderLine.pendingQuantity() + " unidades");
        }
        if (product.isPerishable() && line.expirationDate() == null) {
            throw new IllegalArgumentException(product.getName()
                    + " es perecedero: falta la fecha de vencimiento");
        }
        if (line.expirationDate() != null && line.expirationDate().isBefore(today)) {
            throw new IllegalArgumentException(product.getName() + ": la fecha de vencimiento "
                    + line.expirationDate() + " ya pasó");
        }
    }

    private static PurchaseOrderLine lineOf(PurchaseOrder order, Long productId) {
        return order.getLines().stream()
                .filter(l -> l.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El producto " + productId
                        + " no está en el pedido " + order.getId()));
    }

    private static int pendingUnits(PurchaseOrder order) {
        return order.getLines().stream().mapToInt(PurchaseOrderLine::pendingQuantity).sum();
    }

    // ---------- Pendientes: se implementan D7 ----------



    @Override
    public PurchaseOrder updateOrderLines(Long orderId, List<OrderLineEdit> edits) {
        throw new UnsupportedOperationException("Pendiente: D7");
    }

    @Override
    public PurchaseOrder approveOrder(Long orderId, Long adminUserId) {
        throw new UnsupportedOperationException("Pendiente: D7");
    }

    @Override
    public SupplierMessage buildSupplierMessage(Long orderId) {
        throw new UnsupportedOperationException("Pendiente: D7");
    }

    @Override
    public PurchaseOrder sendOrder(Long orderId, Long adminUserId) {
        throw new UnsupportedOperationException("Pendiente: D7");
    }

    
}