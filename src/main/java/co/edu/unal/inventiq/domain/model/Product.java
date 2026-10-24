package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Producto del catálogo, de solo lectura para la venta. D4 agrega el registro y la edición.
 *
 * @param supplierId   null si no tiene proveedor
 * @param reorderPoint null mientras el motor (C5) no lo haya calculado
 */
public record Product(Long id, String barcode, String name, BigDecimal salePrice, BigDecimal cost, boolean perishable,
                      Long supplierId, int safetyStock, Integer reorderPoint, boolean active) {

    public Product {
        Objects.requireNonNull(barcode, "barcode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(salePrice, "salePrice");
    }
}
