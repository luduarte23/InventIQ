package com.inventiq.application.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.port.AlertRepository;

/** Lee y resuelve alertas por el puerto AlertRepository. Solo orquesta: la regla vive en Alert. */
@Service
public class AlertInboxServiceImpl implements AlertInboxService {

    private final AlertRepository alerts;

    public AlertInboxServiceImpl(AlertRepository alerts) {
        this.alerts = alerts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Alert> unresolvedAlerts() {
        return alerts.findUnresolved();
    }
    
    @Override
    @Transactional
    public void resolve(Long alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("La alerta " + alertId + " no existe"));
        alert.resolve();
        alerts.save(alert);
    }
}