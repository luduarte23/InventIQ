package co.edu.unal.inventiq.domain.exception;

import co.edu.unal.inventiq.domain.model.OrderStatus;

public class OrderNotEditableException extends BusinessException {

    public OrderNotEditableException(OrderStatus status) {
        super("Solo se pueden editar pedidos en borrador; este está \"" + status.label() + "\".");
    }
}
