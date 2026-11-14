package com.inventiq.fakes;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.port.AlertRepository;
import java.util.ArrayList;
import java.util.List;

/** Repositorio falso de alertas: guarda las alertas en una lista. Solo para pruebas. */
public class InMemoryAlertRepository implements AlertRepository {

    private final List<Alert> alerts = new ArrayList<>();
    private long nextId = 1;

    @Override
    public Alert save(Alert alert) {
        Alert saved = alert.getId() != null ? alert : new Alert(nextId++, alert.getCreatedAt(),
                alert.getType(), alert.getMessage(), alert.getRelatedEntityType(),
                alert.getRelatedEntityId(), alert.isResolved());
        alerts.removeIf(a -> a.getId().equals(saved.getId()));
        alerts.add(saved);
        return saved;
    }

    @Override
    public List<Alert> findUnresolved() {
        return alerts.stream().filter(a -> !a.isResolved()).toList();
    }

    public List<Alert> all() {
        return List.copyOf(alerts);
    }
}
