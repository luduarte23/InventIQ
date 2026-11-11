package com.inventiq.web.pos;

import com.inventiq.application.pos.ScanResult;
import com.inventiq.domain.model.SaleItem;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO de salida: la venta en curso tal como se muestra en pantalla. Se guarda en la sesión
 * del cajero mientras la venta está abierta, y se arma a partir de cada ScanResult.
 */
public class SaleScreen {

    private final Long saleId;
    private final List<SaleLineView> lines = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;

    public SaleScreen(Long saleId) {
        this.saleId = saleId;
    }

    /** Agrega a la pantalla las líneas de un escaneo (una por lote) y actualiza el total. */
    public void addScan(ScanResult result) {
        String productName = result.product().getName();
        for (SaleItem item : result.items()) {
            lines.add(new SaleLineView(productName, item.getQuantity(), item.getUnitPrice(), item.subtotal()));
        }
        total = result.saleRunningTotal();
    }

    public Long getSaleId() {
        return saleId;
    }

    public List<SaleLineView> getLines() {
        return lines;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
