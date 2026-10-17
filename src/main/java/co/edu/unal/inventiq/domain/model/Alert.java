package co.edu.unal.inventiq.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Alerta para el administrador.
 *
 * @param id       null hasta que se guarda
 * @param resolved true cuando el administrador ya la atendió
 */
public record Alert(Long id, AlertType type, String message, LocalDateTime createdAt, boolean resolved) {

    public Alert {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Alerta nueva, sin resolver. */
    public static Alert raise(AlertType type, String message, LocalDateTime at) {
        return new Alert(null, type, message, at, false);
    }

    public Alert resolve() {
        return new Alert(id, type, message, createdAt, true);
    }
}
