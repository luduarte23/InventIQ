package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.AuditLog;
import com.inventiq.domain.port.AuditLogRepository;
import com.inventiq.infrastructure.persistence.entity.AuditLogEntity;
import com.inventiq.infrastructure.persistence.jpa.AuditLogJpaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/** Implementa el puerto AuditLogRepository del dominio usando JPA (A5). */
@Repository
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpa;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditLog save(AuditLog entry) {
        return jpa.save(AuditLogEntity.fromDomain(entry)).toDomain();
    }

    /** Los 'limit' registros más recientes. Con limit 0 o negativo no hay nada que traer. */
    @Override
    public List<AuditLog> findRecent(int limit) {
        List<AuditLog> logs = new ArrayList<>();
        if (limit <= 0) {
            return logs;
        }
        for (AuditLogEntity entity : jpa.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(0, limit))) {
            logs.add(entity.toDomain());
        }
        return logs;
    }
}
