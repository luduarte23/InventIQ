package co.edu.unal.inventiq.domain.exception;

import java.util.Map;

/**
 * Otro producto ya tiene ese código. CatalogService lo revisa antes de guardar; esta
 * excepción cubre el caso de dos registros simultáneos, que detecta la base de datos.
 */
public class DuplicateBarcodeException extends InvalidProductException {

    public DuplicateBarcodeException(String barcode) {
        super(Map.of("barcode", "Ya existe un producto con el código " + barcode + "."));
    }
}
