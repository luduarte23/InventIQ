/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Alert {

    private static final int MAX_MESSAGE = 500;

    private Long id;
    private Instant createdAt;
    private AlertType type;
    private String message;
    private String relatedEntityType;   // ej. "Product"; null si la alerta es general
    private Long relatedEntityId;
    private boolean resolved;

    public Alert(Long id, Instant createdAt, AlertType type, String message,
                 String relatedEntityType, Long relatedEntityId, boolean resolved) {
        this.id = id;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        this.type = Objects.requireNonNull(type, "type es obligatorio");
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es obligatorio");
        }
        if (message.length() > MAX_MESSAGE) {
            throw new IllegalArgumentException("message no puede superar "
                    + MAX_MESSAGE + " caracteres");
        }
        if ((relatedEntityType == null) != (relatedEntityId == null)) {
            throw new IllegalArgumentException(
                    "relatedEntityType y relatedEntityId van juntos: los dos o ninguno");
        }
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.resolved = resolved;
    }

    /** Crea una alerta nueva, sin resolver y sin id. */
    public static Alert raise(AlertType type, String message, String relatedEntityType,
                              Long relatedEntityId, Instant now) {
        return new Alert(null, now, type, message, relatedEntityType, relatedEntityId, false);
    }

    public void resolve() {
        this.resolved = true;
    }

    public boolean isAbout(String entityType, Long entityId) {
        return Objects.equals(relatedEntityType, entityType)
                && Objects.equals(relatedEntityId, entityId);
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public AlertType getType() { return type; }
    public String getMessage() { return message; }
    public String getRelatedEntityType() { return relatedEntityType; }
    public Long getRelatedEntityId() { return relatedEntityId; }
    public boolean isResolved() { return resolved; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Alert other)) return false;
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
