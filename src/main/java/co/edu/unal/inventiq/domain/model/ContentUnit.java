package co.edu.unal.inventiq.domain.model;

/** Unidad del contenido neto de un producto: peso, volumen o unidades por empaque. */
public enum ContentUnit {

    G("g", "gramos", false),
    KG("kg", "kilogramos", false),
    ML("ml", "mililitros", false),
    L("L", "litros", false),
    UND("und", "unidades", true);

    private final String symbol;
    private final String label;
    private final boolean wholeUnitsOnly;

    ContentUnit(String symbol, String label, boolean wholeUnitsOnly) {
        this.symbol = symbol;
        this.label = label;
        this.wholeUnitsOnly = wholeUnitsOnly;
    }

    public String symbol() {
        return symbol;
    }

    public String label() {
        return label;
    }

    /** Las unidades por empaque se cuentan enteras: "6 und", nunca "6,5 und". */
    public boolean wholeUnitsOnly() {
        return wholeUnitsOnly;
    }
}
