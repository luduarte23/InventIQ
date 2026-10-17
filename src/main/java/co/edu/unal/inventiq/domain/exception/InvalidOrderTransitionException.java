package co.edu.unal.inventiq.domain.exception;

import co.edu.unal.inventiq.domain.model.OrderStatus;

public class InvalidOrderTransitionException extends BusinessException {

    public InvalidOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("Un pedido en estado \"" + from.label() + "\" no puede pasar a \"" + to.label() + "\".");
    }
}
