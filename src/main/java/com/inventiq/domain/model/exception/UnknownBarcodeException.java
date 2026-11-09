/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model.exception;

public class UnknownBarcodeException extends DomainException {

    private final String barcode;

    public UnknownBarcodeException(String barcode) {
        super("No existe ningún producto con el código de barras " + barcode);
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}