package com.inventiq.application.catalog;

import java.math.BigDecimal;

/** Datos de un producto tal como llegan del formulario de catálogo (D4). supplierId puede ser null. */
public record ProductData(String barcode, String name, BigDecimal salePrice, BigDecimal unitCost,
                          boolean perishable, Long supplierId, int safetyStock) {}