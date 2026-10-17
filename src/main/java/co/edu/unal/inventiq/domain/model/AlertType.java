package co.edu.unal.inventiq.domain.model;

/** Tipos de alerta que ve el administrador en el inicio. */
public enum AlertType {

    /** Hay productos bajo el punto de reorden sin proveedor (US-01 esc. 2). */
    ORDER_NEEDS_REVIEW("Pedido por revisar"),
    /** No se pudo enviar un mensaje (riesgo de Twilio en la Entrega 1). */
    NOTIFICATION_FAILED("Mensaje no enviado"),
    /** Un lote está por vencer y no hay historial para calcular el descuento (US-04 esc. 2, Entrega 3). */
    EXPIRING_BATCH("Lote por vencer");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
