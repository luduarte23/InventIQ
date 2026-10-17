package co.edu.unal.inventiq.domain.model;

/** Rol de un usuario (FR-01). El cajero solo registra ventas; el administrador hace todo. */
public enum Role {

    ADMIN("Administrador"),
    CASHIER("Cajero");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
