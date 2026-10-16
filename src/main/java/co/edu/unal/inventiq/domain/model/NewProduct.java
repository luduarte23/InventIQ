package co.edu.unal.inventiq.domain.model;

import java.time.LocalDate;

/**
 * Un producto por registrar: su código, sus datos y, opcionalmente, su stock inicial.
 *
 * @param saleEnabled       false para registrarlo con la venta pausada (p. ej. stock comprado
 *                          antes de un alza que aún no se quiere vender)
 * @param initialQuantity   unidades del primer lote; null o 0 para registrarlo sin stock
 * @param initialExpiration vencimiento de ese primer lote; solo para perecederos
 */
public record NewProduct(
        String barcode,
        ProductData data,
        boolean saleEnabled,
        Integer initialQuantity,
        LocalDate initialExpiration) {

    public boolean hasInitialStock() {
        return initialQuantity != null && initialQuantity > 0;
    }

    /** El producto a guardar. Solo tiene sentido después de validar. */
    public Product toProduct() {
        return Product.register(barcode, data, saleEnabled);
    }
}
