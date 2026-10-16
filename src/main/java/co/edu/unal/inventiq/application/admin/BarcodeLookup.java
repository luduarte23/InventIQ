package co.edu.unal.inventiq.application.admin;

/** Qué pasa con un código escaneado (o escrito) en la pantalla de productos. */
public sealed interface BarcodeLookup {

    /** El código no es un GTIN válido; {@code reason} explica por qué. */
    record Invalid(String barcode, String reason) implements BarcodeLookup {
    }

    /** Ya hay un producto con ese código. */
    record AlreadyRegistered(CatalogEntry entry) implements BarcodeLookup {
    }

    /** Código válido y libre: se puede registrar un producto nuevo con él. */
    record Available(String barcode) implements BarcodeLookup {
    }
}
