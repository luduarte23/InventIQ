package com.inventiq.web.pos;

import java.math.BigDecimal;

/**
 * DTO de salida: una línea tal como la ve el cajero en la pantalla. Lleva el nombre del
 * producto (el SaleItem del dominio solo tiene su id) y nada interno como el lote o el costo.
 */
public record SaleLineView(String productName, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
}
