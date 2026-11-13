package com.inventiq.web.pos;

/**
 * DTO de entrada: lo que el cajero envía en el formulario de la pantalla de ventas.
 * Spring lo llena solo con @ModelAttribute, usando los nombres de los campos del formulario.
 */
public class ScanForm {

    private String barcode;
    private int quantity = 1;

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
