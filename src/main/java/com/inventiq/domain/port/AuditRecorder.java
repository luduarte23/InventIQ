/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.inventiq.domain.port;

import com.inventiq.domain.model.AuditAction;

public interface AuditRecorder {

    /** actorUserIdOrNull = null cuando la acción la ejecuta el scheduler (actor SYSTEM). */
    void record(AuditAction action, String entityType, Long entityId, String details,
                Long actorUserIdOrNull);
}
