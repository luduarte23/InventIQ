/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.domain.model;

import java.time.Instant;
import java.util.Objects;

public record AuditLog(
        Long id,
        Instant createdAt,
        Long actorUserId,      // null si la acción la hizo el sistema (tarea automática)
        AuditAction action,
        String entityType,
        Long entityId,
        String details) {

    private static final int MAX_DETAILS = 2000;

    public AuditLog {
        Objects.requireNonNull(createdAt, "createdAt es obligatorio");
        Objects.requireNonNull(action, "action es obligatorio");
        Objects.requireNonNull(entityId, "entityId es obligatorio");
        if (entityType == null || entityType.isBlank()) {
            throw new IllegalArgumentException("entityType es obligatorio");
        }
        if (details != null && details.length() > MAX_DETAILS) {
            details = details.substring(0, MAX_DETAILS);
        }
    }

    /** Crea un registro nuevo, todavía sin id. */
    public static AuditLog of(Long actorUserId, AuditAction action, String entityType,
                              Long entityId, String details, Instant now) {
        return new AuditLog(null, now, actorUserId, action, entityType, entityId, details);
    }
}
