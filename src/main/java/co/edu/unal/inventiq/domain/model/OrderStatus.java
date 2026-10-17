package co.edu.unal.inventiq.domain.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un pedido de compra: {@code DRAFT → APPROVED → SENT → PARTIALLY_RECEIVED → RECEIVED}.
 * Un pedido recibido en parte puede recibir más entregas parciales antes de completarse.
 */
public enum OrderStatus {

    DRAFT("Borrador"),
    APPROVED("Aprobado"),
    SENT("Enviado"),
    PARTIALLY_RECEIVED("Recibido en parte"),
    RECEIVED("Recibido");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Estados a los que se puede pasar desde este. */
    public Set<OrderStatus> nextStatuses() {
        return switch (this) {
            case DRAFT -> EnumSet.of(APPROVED);
            case APPROVED -> EnumSet.of(SENT);
            case SENT, PARTIALLY_RECEIVED -> EnumSet.of(PARTIALLY_RECEIVED, RECEIVED);
            case RECEIVED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canTransitionTo(OrderStatus next) {
        return nextStatuses().contains(next);
    }

    /** Solo un borrador se puede editar. */
    public boolean isEditable() {
        return this == DRAFT;
    }

    /** El pedido ya salió hacia el proveedor y espera mercancía. */
    public boolean isAwaitingReceipt() {
        return this == SENT || this == PARTIALLY_RECEIVED;
    }
}
