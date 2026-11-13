package com.inventiq.infrastructure.persistence.entity;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Fila de la tabla 'audit_logs'. La acción se guarda como texto (VARCHAR), igual que en V1.
 * actor_user_id es null cuando la acción la hizo el scheduler (actor SYSTEM).
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "details")
    private String details;

    /** JPA necesita un constructor vacío. */
    protected AuditLogEntity() {
    }

    /** Del dominio a la tabla. AuditLog es un record: sus getters no llevan "get". */
    public static AuditLogEntity fromDomain(AuditLog log) {
        AuditLogEntity entity = new AuditLogEntity();
        entity.id = log.id();
        entity.createdAt = log.createdAt();
        entity.actorUserId = log.actorUserId();
        entity.action = log.action().name();
        entity.entityType = log.entityType();
        entity.entityId = log.entityId();
        entity.details = log.details();
        return entity;
    }

    /** De la tabla al dominio. */
    public AuditLog toDomain() {
        return new AuditLog(id, createdAt, actorUserId, AuditAction.valueOf(action),
                entityType, entityId, details);
    }
}