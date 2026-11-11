package com.inventiq.domain.model.exception;

/**
 * Error de negocio de la venta: cantidad no válida, venta que no existe, venta ya cerrada
 * o venta sin productos. El mensaje se le muestra tal cual al cajero.
 */
public class InvalidSaleException extends DomainException {

    public InvalidSaleException(String message) {
        super(message);
    }
}
