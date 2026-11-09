/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model.exception;

public class InsufficientStockException extends DomainException {

    private final Long productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Stock insuficiente para el producto " + productId
                + ": se pidieron " + requested + " y hay " + available);
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Long getProductId() { return productId; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
