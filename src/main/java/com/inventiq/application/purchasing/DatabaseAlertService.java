package com.inventiq.application.purchasing;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import com.inventiq.domain.port.AlertRepository;
import com.inventiq.domain.port.AlertService;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Guarda las alertas que el administrador debe ver. Si ya hay una alerta sin resolver del mismo
 * tipo y sobre lo mismo, no crea otra: así el análisis se puede repetir sin llenar la lista.
 * Versión simple, igual que DatabaseAuditRecorder.
 */
@Service
public class DatabaseAlertService implements AlertService {

    private final AlertRepository alertRepository;
    private final Clock clock;

    public DatabaseAlertService(AlertRepository alertRepository, Clock clock) {
        this.alertRepository = alertRepository;
        this.clock = clock;
    }

    @Override
    public void raise(AlertType type, String message, String relatedEntityType,
                      Long relatedEntityId) {
        for (Alert open : alertRepository.findUnresolved()) {
            if (open.getType() == type && open.isAbout(relatedEntityType, relatedEntityId)) {
                return;
            }
        }
        alertRepository.save(Alert.raise(type, message, relatedEntityType, relatedEntityId,
                Instant.now(clock)));
    }
}
