package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Fila de la tabla 'alerts'. El tipo se guarda como texto (VARCHAR), igual que en V1. */
@Entity
@Table(name = "alerts")
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "related_entity_type")
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    /** JPA necesita un constructor vacío. */
    protected AlertEntity() {
    }

    /** Del dominio a la tabla. */
    public static AlertEntity fromDomain(Alert alert) {
        AlertEntity entity = new AlertEntity();
        entity.id = alert.getId();
        entity.createdAt = alert.getCreatedAt();
        entity.type = alert.getType().name();
        entity.message = alert.getMessage();
        entity.relatedEntityType = alert.getRelatedEntityType();
        entity.relatedEntityId = alert.getRelatedEntityId();
        entity.resolved = alert.isResolved();
        return entity;
    }

    /** De la tabla al dominio. */
    public Alert toDomain() {
        return new Alert(id, createdAt, AlertType.valueOf(type), message,
                relatedEntityType, relatedEntityId, resolved);
    }
}