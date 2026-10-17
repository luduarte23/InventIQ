package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;

/**
 * Línea de un pedido de compra: un producto, cuántas unidades se pidieron y cuántas han llegado.
 * Las cantidades solo cambian a través de {@link PurchaseOrder}, que vigila el estado del pedido.
 */
public class OrderLine {

    private final Long id;
    private final long productId;
    private int quantityOrdered;
    private int quantityReceived;
    private final BigDecimal unitCost;

    /** @param id null hasta que se guarda */
    public OrderLine(Long id, long productId, int quantityOrdered, int quantityReceived, BigDecimal unitCost) {
        if (quantityOrdered <= 0) {
            throw new InvalidQuantityException(quantityOrdered);
        }
        if (quantityReceived < 0 || quantityReceived > quantityOrdered) {
            throw new IllegalArgumentException("Cantidad recibida fuera de rango: " + quantityReceived);
        }
        Objects.requireNonNull(unitCost, "unitCost");
        if (unitCost.signum() < 0) {
            throw new IllegalArgumentException("El costo unitario no puede ser negativo: " + unitCost);
        }
        this.id = id;
        this.productId = productId;
        this.quantityOrdered = quantityOrdered;
        this.quantityReceived = quantityReceived;
        this.unitCost = unitCost;
    }

    /** Línea nueva de un borrador, sin unidades recibidas. */
    public static OrderLine of(long productId, int quantity, BigDecimal unitCost) {
        return new OrderLine(null, productId, quantity, 0, unitCost);
    }

    public BigDecimal subtotal() {
        return unitCost.multiply(BigDecimal.valueOf(quantityOrdered));
    }

    /** Unidades que faltan por llegar. */
    public int pending() {
        return quantityOrdered - quantityReceived;
    }

    public boolean isComplete() {
        return pending() == 0;
    }

    void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        quantityOrdered = quantity;
    }

    void receive(int quantity) {
        quantityReceived += quantity;
    }

    public Long getId() {
        return id;
    }

    public long getProductId() {
        return productId;
    }

    public int getQuantityOrdered() {
        return quantityOrdered;
    }

    public int getQuantityReceived() {
        return quantityReceived;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }
}
