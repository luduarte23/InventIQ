package co.edu.unal.inventiq.domain.port;

import java.util.List;

import co.edu.unal.inventiq.domain.model.AuditEntry;

/** Puerto de la auditoría (FR-11). Solo se agrega: los registros nunca se editan. */
public interface AuditLogRepository {

    void record(AuditEntry entry);

    /** Los últimos registros, del más reciente al más antiguo. */
    List<AuditEntry> findRecent(int limit);
}
