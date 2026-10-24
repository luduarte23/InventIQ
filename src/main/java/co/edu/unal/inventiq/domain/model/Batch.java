package co.edu.unal.inventiq.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;

/**
 * Lote de un producto: unidades recibidas juntas, con una misma fecha de vencimiento.
 * El código de barras identifica al producto, no al lote; por eso la venta decide
 * de qué lote descontar con FifoAllocator.
 */
public class Batch {

    private final Long id;
    private final long productId;
    private final int quantityReceived;
    private int quantityAvailable;
    private final LocalDate expirationDate;
    private final LocalDateTime receivedAt;
    private final long version;

    /**
     * @param expirationDate null para productos no perecederos
     * @param version        versión de bloqueo optimista leída de la base; se devuelve
     *                       intacta al guardar para detectar cambios concurrentes
     */
    public Batch(Long id, long productId, int quantityReceived, int quantityAvailable,
                 LocalDate expirationDate, LocalDateTime receivedAt, long version) {
        if (quantityAvailable < 0 || quantityAvailable > quantityReceived) {
            throw new IllegalArgumentException("Cantidad disponible fuera de rango: " + quantityAvailable);
        }
        this.id = id;
        this.productId = productId;
        this.quantityReceived = quantityReceived;
        this.quantityAvailable = quantityAvailable;
        this.expirationDate = expirationDate;
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        this.version = version;
    }

    /** Un lote vence al terminar su fecha de vencimiento: el mismo día todavía se vende. */
    public boolean isExpired(LocalDate today) {
        return expirationDate != null && expirationDate.isBefore(today);
    }

    public boolean isSellable(LocalDate today) {
        return quantityAvailable > 0 && !isExpired(today);
    }

    public ExpirationStatus expirationStatus(LocalDate today) {
        return ExpirationStatus.classify(expirationDate, today);
    }

    /** Días que faltan para vencer (negativo si ya venció), o null si el lote no vence. */
    public Long daysUntilExpiration(LocalDate today) {
        return expirationDate == null ? null : ChronoUnit.DAYS.between(today, expirationDate);
    }

    public void deduct(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        if (quantity > quantityAvailable) {
            throw new IllegalStateException("El lote " + id + " solo tiene " + quantityAvailable
                    + " unidades y se intentaron descontar " + quantity);
        }
        quantityAvailable -= quantity;
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public int getQuantityAvailable() {
        return quantityAvailable;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public long getVersion() {
        return version;
    }
}
