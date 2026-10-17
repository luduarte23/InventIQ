package co.edu.unal.inventiq.domain.model;

/** En qué se mide el pedido mínimo de un proveedor (MOQ, FR-05). */
public enum MinimumOrderType {

    /** Monto total del pedido en pesos. */
    AMOUNT,
    /** Unidades totales del pedido. */
    UNITS
}
