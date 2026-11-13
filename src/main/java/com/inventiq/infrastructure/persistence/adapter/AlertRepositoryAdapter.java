package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.port.AlertRepository;
import com.inventiq.infrastructure.persistence.entity.AlertEntity;
import com.inventiq.infrastructure.persistence.jpa.AlertJpaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Implementa el puerto AlertRepository del dominio usando JPA (A5). */
@Repository
public class AlertRepositoryAdapter implements AlertRepository {

    private final AlertJpaRepository jpa;

    public AlertRepositoryAdapter(AlertJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Alert save(Alert alert) {
        return jpa.save(AlertEntity.fromDomain(alert)).toDomain();
    }

    @Override
    public List<Alert> findUnresolved() {
        List<Alert> alerts = new ArrayList<>();
        for (AlertEntity entity : jpa.findByResolvedFalseOrderByCreatedAtDesc()) {
            alerts.add(entity.toDomain());
        }
        return alerts;
    }
}
