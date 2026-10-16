package co.edu.unal.inventiq.domain.model;

import java.math.BigDecimal;

/**
 * Los datos editables de un producto, tal como llegan del formulario: cualquier campo puede
 * venir vacío (null). NewProductValidator los revisa, y los nombres de estos componentes son
 * las claves de sus errores. El código de barras no está aquí porque nunca se edita.
 *
 * @param netContentAmount cuánto trae el empaque; va junto con {@code contentUnit}
 * @param supplierId       null si el producto todavía no tiene proveedor
 */
public record ProductData(
        String name,
        ProductCategory category,
        String brand,
        BigDecimal netContentAmount,
        ContentUnit contentUnit,
        String description,
        BigDecimal salePrice,
        BigDecimal cost,
        boolean perishable,
        Long supplierId,
        Integer safetyStock) {

    /** El contenido neto, o null si no se indicó. Solo tiene sentido después de validar. */
    public NetContent netContent() {
        if (netContentAmount == null || contentUnit == null) {
            return null;
        }
        return new NetContent(netContentAmount, contentUnit);
    }

    public int safetyStockOrZero() {
        return safetyStock == null ? 0 : safetyStock;
    }
}
