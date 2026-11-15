package com.inventiq.web.admin;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import com.inventiq.application.admin.AlertInboxService;
import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;

/** Pruebas del controlador con un servicio falso: sin servidor ni base de datos. */
class AdminHomeControllerTest {

    private final FakeService service = new FakeService();
    private final AdminHomeController controller = new AdminHomeController(service);

    @Test
    void theHomeShowsTheUnresolvedAlerts() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("admin/home", controller.home(model));
        assertEquals(service.alerts, model.get("alerts"));
    }

    /** Servicio falso: una alerta fija. */
    static class FakeService implements AlertInboxService {
        final List<Alert> alerts = List.of(new Alert(7L, Instant.parse("2026-11-15T15:00:00Z"),
                AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, false));

        public List<Alert> unresolvedAlerts() { return alerts; }
    }
}