/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
// ScanResult.java
package com.inventiq.application.pos;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.SaleItem;
import java.math.BigDecimal;
import java.util.List;

public record ScanResult(Product product, List<SaleItem> items, BigDecimal saleRunningTotal) {

    public ScanResult {
        items = List.copyOf(items);   // copia inmutable
    }
}
