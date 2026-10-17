package co.edu.unal.inventiq.domain.model;

/** Qué se registró en la auditoría (FR-11): cambios manuales y decisiones del motor. */
public enum AuditAction {

    SETTINGS_CHANGED("Parámetros cambiados"),
    STOCK_ADJUSTED("Stock ajustado"),
    REORDER_POINTS_RECALCULATED("Puntos de reorden recalculados"),
    DRAFT_ORDERS_GENERATED("Borradores generados"),
    ORDER_APPROVED("Pedido aprobado"),
    ORDER_SENT("Pedido enviado"),
    ORDER_RECEIVED("Pedido recibido");

    private final String label;

    AuditAction(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
