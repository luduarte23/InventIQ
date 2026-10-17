package co.edu.unal.inventiq.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Registro de auditoría.
 *
 * @param id       null hasta que se guarda
 * @param details  descripción legible de lo que pasó; null si no hace falta
 * @param username null si la decisión fue automática (motor de reabastecimiento)
 */
public record AuditEntry(Long id, AuditAction action, String details, String username, LocalDateTime createdAt) {

    public AuditEntry {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    /** Decisión tomada por el sistema, sin usuario. */
    public static AuditEntry automatic(AuditAction action, String details, LocalDateTime at) {
        return new AuditEntry(null, action, details, null, at);
    }

    /** Acción hecha por un usuario. */
    public static AuditEntry byUser(String username, AuditAction action, String details, LocalDateTime at) {
        return new AuditEntry(null, action, details, Objects.requireNonNull(username, "username"), at);
    }

    public boolean isAutomatic() {
        return username == null;
    }
}
