package com.inventiq.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventiq.application.admin.AlertInboxService;
import com.inventiq.domain.model.Alert;
import com.inventiq.domain.model.AlertType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

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

    @Test
    void resolvingAnAlertDelegatesToTheServiceAndShowsAMessage() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/admin", controller.resolve(7L, redirect));
        assertEquals(List.of(7L), service.resolved);
        assertEquals("Alerta marcada como resuelta.", redirect.getFlashAttributes().get("message"));
    }

    @Test
    void anUnknownAlertShowsTheErrorInsteadOfFailing() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/admin", controller.resolve(999L, redirect));
        assertEquals("La alerta 999 no existe", redirect.getFlashAttributes().get("error"));
    }

    /** Servicio falso: una alerta fija; la 999 no existe. */
    static class FakeService implements AlertInboxService {
        final List<Alert> alerts = List.of(new Alert(7L, Instant.parse("2026-11-15T15:00:00Z"),
                AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, false));
        final List<Long> resolved = new ArrayList<>();

        public List<Alert> unresolvedAlerts() { return alerts; }
        public void resolve(Long alertId) {
            if (alertId == 999L) {
                throw new IllegalArgumentException("La alerta 999 no existe");
            }
            resolved.add(alertId);
        }
    }
}