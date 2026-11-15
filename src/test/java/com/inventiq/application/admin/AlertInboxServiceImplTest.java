package com.inventiq.application.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import com.inventiq.fakes.InMemoryAlertRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;


/** Pruebas de la bandeja de alertas con el repositorio falso: sin Spring ni base de datos. */
class AlertInboxServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-15T15:00:00Z");

    private final InMemoryAlertRepository alerts = new InMemoryAlertRepository();
    private final AlertInboxServiceImpl service = new AlertInboxServiceImpl(alerts);

    @Test
    void theInboxShowsOnlyUnresolvedAlerts() {
        Alert open = alerts.save(Alert.raise(AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, NOW));
        Alert done = alerts.save(Alert.raise(AlertType.NOTIFICATION_FAILED, "No se envió", null, null, NOW));
        done.resolve();
        alerts.save(done);

        List<Alert> inbox = service.unresolvedAlerts();

        assertEquals(List.of(open), inbox);
    }
    
    @Test
    void resolvingAnAlertTakesItOutOfTheInbox() {
        Alert open = alerts.save(Alert.raise(AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, NOW));

        service.resolve(open.getId());

        assertTrue(service.unresolvedAlerts().isEmpty());
        assertTrue(alerts.findById(open.getId()).get().isResolved());
    }

    @Test
    void resolvingAnUnknownAlertFails() {
        assertThrows(IllegalArgumentException.class, () -> service.resolve(999L));
    }
}