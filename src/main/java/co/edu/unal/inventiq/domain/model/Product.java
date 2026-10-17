package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Producto del catálogo.
 *
 * @param brand        null si no tiene marca (p. ej. productos a granel)
 * @param netContent   null si no se registró el contenido
 * @param description  null si no tiene descripción
 * @param supplierId   null si el producto no tiene proveedor asignado
 * @param reorderPoint null mientras el motor (C5) no lo haya calculado
 * @param active       false si el producto se dejó de manejar (D4)
 * @param saleEnabled  false si la venta está pausada: sigue en el inventario, pero el POS no lo vende
 */
public record Product(
        Long id,
        String barcode,
        String name,
        ProductCategory category,
        String brand,
        NetContent netContent,
        String description,
        BigDecimal salePrice,
        BigDecimal cost,
        boolean perishable,
        Long supplierId,
        int safetyStock,
        Integer reorderPoint,
        boolean active,
        boolean saleEnabled) {

    public Product {
        Objects.requireNonNull(barcode, "barcode");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(salePrice, "salePrice");
        Objects.requireNonNull(cost, "cost");
    }

    /** Producto nuevo, activo, a partir de datos ya validados. */
    public static Product register(String barcode, ProductData data, boolean saleEnabled) {
        return new Product(null, barcode.strip(), data.name().strip(), data.category(), clean(data.brand()),
                data.netContent(), clean(data.description()), data.salePrice(), data.cost(), data.perishable(),
                data.supplierId(), data.safetyStockOrZero(), null, true, saleEnabled);
    }

    /** El mismo producto con los datos editados (ya validados). El código no cambia nunca. */
    public Product withData(ProductData data) {
        return new Product(id, barcode, data.name().strip(), data.category(), clean(data.brand()), data.netContent(),
                clean(data.description()), data.salePrice(), data.cost(), data.perishable(), data.supplierId(),
                data.safetyStockOrZero(), reorderPoint, active, saleEnabled);
    }

    public Product withSaleEnabled(boolean enabled) {
        return new Product(id, barcode, name, category, brand, netContent, description, salePrice, cost, perishable,
                supplierId, safetyStock, reorderPoint, active, enabled);
    }

    /** Se puede vender: está activo y su venta no está pausada. */
    public boolean isSellable() {
        return active && saleEnabled;
    }

    /** true si el precio de venta o el costo cambiaron respecto a {@code other}. */
    public boolean pricesDifferFrom(Product other) {
        return salePrice.compareTo(other.salePrice) != 0 || cost.compareTo(other.cost) != 0;
    }

    private static String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.strip();
    }
}
