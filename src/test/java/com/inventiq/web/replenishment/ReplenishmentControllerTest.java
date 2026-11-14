package com.inventiq.web.replenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventiq.application.replenishment.BudgetResult;
import com.inventiq.application.replenishment.OrderLineSuggestion;
import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.application.replenishment.ReplenishmentService;
import com.inventiq.domain.model.PurchaseOrder;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

/** Pruebas del controlador con un servicio falso: sin servidor ni base de datos. */
class ReplenishmentControllerTest {

    private final FakeService service = new FakeService();
    private final ReplenishmentController controller = new ReplenishmentController(service);

    @Test
    void theScreenShowsEveryProductAndHowManyNeedAnOrder() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertEquals("replenishment/index", controller.show(model));
        assertEquals(service.rows, model.get("rows"));
        assertEquals(1L, model.get("toOrder"));
    }

    /** Servicio falso: devuelve dos filas fijas, una que necesita pedido y otra que no. */
    static class FakeService implements ReplenishmentService {
        final List<ReorderStatus> rows = List.of(
                new ReorderStatus(1L, "1001", "Leche", "Alpina", 5, new BigDecimal("2.0000"), 10, true, 19),
                new ReorderStatus(2L, "1002", "Avena", "Alpina", 30, null, 4, false, 0));

        public List<ReorderStatus> reorderOverview() { return rows; }
        public List<Long> recalculateReorderPoints() { return List.of(); }
        public List<PurchaseOrder> generateDraftOrders() { return List.of(); }
        public List<OrderLineSuggestion> suggestMoqCompletion(Long orderId) { return List.of(); }
        public BudgetResult prioritizeUnderBudget(List<Long> ids, BigDecimal budget) { return null; }
    }
}
