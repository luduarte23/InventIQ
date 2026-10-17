package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import co.edu.unal.inventiq.domain.exception.InvalidOrderTransitionException;
import co.edu.unal.inventiq.domain.exception.InvalidReceiptException;
import co.edu.unal.inventiq.domain.exception.OrderNotEditableException;
import co.edu.unal.inventiq.domain.exception.OrderWithoutSupplierException;

/**
 * Pedido de compra a un proveedor. Nace como borrador (FR-04) y solo avanza por transiciones
 * válidas de {@link OrderStatus}. Un pedido sin proveedor queda "por revisar" (US-01 esc. 2) y no
 * se puede aprobar hasta que se le asigne uno.
 */
public class PurchaseOrder {

    private final Long id;
    private final Long supplierId;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private final long version;
    private final List<OrderLine> lines;

    /**
     * @param id         null hasta que se guarda
     * @param supplierId null si es un pedido "por revisar"
     * @param version    versión de bloqueo optimista leída de la base
     */
    public PurchaseOrder(Long id, Long supplierId, OrderStatus status, LocalDateTime createdAt,
                         LocalDateTime approvedAt, LocalDateTime sentAt, LocalDateTime receivedAt,
                         long version, List<OrderLine> lines) {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("Un pedido necesita al menos una línea");
        }
        var products = new HashSet<Long>();
        for (OrderLine line : lines) {
            if (!products.add(line.getProductId())) {
                throw new IllegalArgumentException("El producto " + line.getProductId() + " aparece dos veces");
            }
        }
        this.id = id;
        this.supplierId = supplierId;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.sentAt = sentAt;
        this.receivedAt = receivedAt;
        this.version = version;
        this.lines = List.copyOf(lines);
    }

    /** Borrador nuevo (FR-04). */
    public static PurchaseOrder draft(Long supplierId, List<OrderLine> lines, LocalDateTime createdAt) {
        return new PurchaseOrder(null, supplierId, OrderStatus.DRAFT, createdAt, null, null, null, 0, lines);
    }

    public boolean needsReview() {
        return supplierId == null;
    }

    public BigDecimal total() {
        return lines.stream().map(OrderLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int totalUnits() {
        return lines.stream().mapToInt(OrderLine::getQuantityOrdered).sum();
    }

    /** Líneas a las que aún les faltan unidades (US-05 esc. 2: pendientes visibles). */
    public List<OrderLine> pendingLines() {
        return lines.stream().filter(line -> !line.isComplete()).toList();
    }

    public Optional<OrderLine> lineFor(long productId) {
        return lines.stream().filter(line -> line.getProductId() == productId).findFirst();
    }

    /** Cambia la cantidad pedida de un producto. Solo en borrador. */
    public void changeQuantity(long productId, int quantity) {
        if (!status.isEditable()) {
            throw new OrderNotEditableException(status);
        }
        OrderLine line = lineFor(productId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no tiene el producto " + productId));
        line.changeQuantity(quantity);
    }

    /** DRAFT → APPROVED. */
    public void approve(LocalDateTime now) {
        if (needsReview()) {
            throw new OrderWithoutSupplierException();
        }
        moveTo(OrderStatus.APPROVED);
        approvedAt = now;
    }

    /** APPROVED → SENT (en la Entrega 2 se marca a mano). */
    public void markSent(LocalDateTime now) {
        moveTo(OrderStatus.SENT);
        sentAt = now;
    }

    /**
     * Registra la mercancía que llegó (FR-10, US-05). Queda RECEIVED si ya no falta nada, o
     * PARTIALLY_RECEIVED si quedan pendientes. Crear los lotes le corresponde al servicio.
     *
     * @param receivedByProduct unidades recibidas por id de producto; los productos que no
     *                          aparecen no recibieron nada en esta entrega
     */
    public void receive(Map<Long, Integer> receivedByProduct, LocalDateTime now) {
        if (!status.isAwaitingReceipt()) {
            throw new InvalidOrderTransitionException(status, OrderStatus.RECEIVED);
        }
        int total = 0;
        for (var entry : receivedByProduct.entrySet()) {
            OrderLine line = lineFor(entry.getKey())
                    .orElseThrow(() -> new InvalidReceiptException("El pedido no incluye ese producto."));
            int quantity = entry.getValue();
            if (quantity < 0) {
                throw new InvalidReceiptException("Las cantidades recibidas no pueden ser negativas.");
            }
            if (quantity > line.pending()) {
                throw new InvalidReceiptException("Se recibieron " + quantity + " unidades y solo faltaban "
                        + line.pending() + ".");
            }
            total += quantity;
        }
        if (total == 0) {
            throw new InvalidReceiptException("Indica al menos una unidad recibida.");
        }
        receivedByProduct.forEach((productId, quantity) -> lineFor(productId).orElseThrow().receive(quantity));
        if (pendingLines().isEmpty()) {
            moveTo(OrderStatus.RECEIVED);
            receivedAt = now;
        } else {
            moveTo(OrderStatus.PARTIALLY_RECEIVED);
        }
    }

    private void moveTo(OrderStatus next) {
        if (!status.canTransitionTo(next)) {
            throw new InvalidOrderTransitionException(status, next);
        }
        status = next;
    }

    public Long getId() {
        return id;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public long getVersion() {
        return version;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
