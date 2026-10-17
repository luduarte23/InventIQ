package co.edu.unal.inventiq.domain.exception;

/** US-03 escenario 2: el código escaneado no corresponde a ningún producto activo. */
public class UnknownBarcodeException extends BusinessException {

    private final String barcode;

    public UnknownBarcodeException(String barcode) {
        super("El código " + barcode + " no está registrado. La venta continúa sin ese producto.");
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }
}
