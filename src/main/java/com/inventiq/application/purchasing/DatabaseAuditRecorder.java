package com.inventiq.application.purchasing;

import com.inventiq.domain.model.AuditAction;
import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.port.AuditLogRepository;
import com.inventiq.domain.port.AuditRecorder;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Guarda en la base cada decisión que toma el sistema o el administrador (quién, qué y por qué).
 * Versión simple para que el reabastecimiento pueda registrar sus decisiones; quien lleve la
 * auditoría puede ampliarla o reemplazarla sin que cambie nadie que la use.
 */
@Service
public class DatabaseAuditRecorder implements AuditRecorder {

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public DatabaseAuditRecorder(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Override
    public void record(AuditAction action, String entityType, Long entityId, String details,
                       Long actorUserIdOrNull) {
        auditLogRepository.save(AuditLog.of(actorUserIdOrNull, action, entityType, entityId,
                details, Instant.now(clock)));
    }
}
