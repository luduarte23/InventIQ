/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
// SalesService.java
package com.inventiq.application.pos;

import com.inventiq.domain.model.Sale;

public interface SalesService {

    Sale startSale(Long cashierId);

    /**
     * Descuenta stock de inmediato con FIFO (§5.3).
     * @throws com.inventiq.domain.model.exception.UnknownBarcodeException
     *         no modifica stock ni registra venta (US-03 esc. 2)
     * @throws com.inventiq.domain.model.exception.InsufficientStockException
     */
    ScanResult scan(Long saleId, String barcode, int quantity);

    Sale finishSale(Long saleId);
}
