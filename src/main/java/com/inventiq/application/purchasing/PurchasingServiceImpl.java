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
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Settings;
import com.inventiq.domain.port.AuditRecorder;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.SettingsRepository;


/**
 * Compras y administración (parte D). Lee y guarda por los puertos, deja las reglas al dominio
 * (Settings, Batch, PurchaseOrder) y audita cada cambio administrativo. Solo orquesta.
 */
@Service
public class PurchasingServiceImpl implements PurchasingService {

    private final SettingsRepository settings;
    private final BatchRepository batches;
    private final AuditRecorder audit;
    private final Clock clock;

    public PurchasingServiceImpl(SettingsRepository settings, BatchRepository batches,
                                 AuditRecorder audit, Clock clock) {
        this.settings = settings;
        this.batches = batches;
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

    // ---------- Pendientes: se implementan en los siguientes commits y en D6 y D7 ----------



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

    @Override
    public PurchaseOrder confirmReceipt(Long orderId, List<ReceiptLine> lines, Long adminUserId) {
        throw new UnsupportedOperationException("Pendiente: D6");
    }
}