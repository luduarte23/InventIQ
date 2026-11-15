package com.inventiq.application.catalog;

import java.math.BigDecimal;

/** Datos de un proveedor tal como llegan del formulario (D4). Los dos mínimos pueden ser null. */
public record SupplierData(String name, String whatsappNumber, int leadTimeDays,
                           BigDecimal minOrderAmount, Integer minOrderUnits) {}