package co.edu.unal.inventiq.domain.model;

/**
 * Tipo de producto. Es una lista fija pensada para una tienda de barrio; si la tienda necesita
 * crear sus propios tipos, se puede convertir en una tabla administrable.
 */
public enum ProductCategory {

    LACTEOS("Lácteos"),
    GRANOS("Granos y cereales"),
    ACEITES("Aceites y grasas"),
    ENDULZANTES("Azúcar y panela"),
    CAFE("Café, té y chocolate"),
    PANADERIA("Panadería"),
    BEBIDAS("Bebidas"),
    ENLATADOS("Enlatados y conservas"),
    SNACKS("Snacks y galletas"),
    CARNES("Carnes, huevos y embutidos"),
    FRUTAS_VERDURAS("Frutas y verduras"),
    ASEO("Aseo y limpieza"),
    OTROS("Otros");

    private final String label;

    ProductCategory(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
