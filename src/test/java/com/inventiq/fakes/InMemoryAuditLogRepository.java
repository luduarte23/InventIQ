package com.inventiq.fakes;

import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.port.AuditLogRepository;
import java.util.ArrayList;
import java.util.List;

/** Repositorio falso de auditoría: guarda los registros en una lista. Solo para pruebas. */
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final List<AuditLog> entries = new ArrayList<>();
    private long nextId = 1;

    @Override
    public AuditLog save(AuditLog entry) {
        AuditLog saved = new AuditLog(nextId++, entry.createdAt(), entry.actorUserId(),
                entry.action(), entry.entityType(), entry.entityId(), entry.details());
        entries.add(saved);
        return saved;
    }

    /** Del más reciente al más antiguo, como el repositorio real. */
    @Override
    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> recent = new ArrayList<>(entries.reversed());
        return recent.subList(0, Math.min(Math.max(limit, 0), recent.size()));
    }

    public List<AuditLog> all() {
        return List.copyOf(entries);
    }
}
