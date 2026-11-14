package com.inventiq.web.replenishment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventiq.application.replenishment.BudgetResult;
import com.inventiq.application.replenishment.OrderLineSuggestion;
import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.application.replenishment.ReplenishmentService;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/** Pruebas del controlador con un servicio falso: sin servidor ni base de datos. */
@SuppressWarnings("unchecked")
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

    @Test
    void theButtonGeneratesTheDraftsAndShowsThemWithTheirSuggestions() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        assertEquals("redirect:/replenishment", controller.generateDrafts(redirect));
        assertEquals("2 puntos de reorden actualizados y 1 borradores creados o ampliados.",
                redirect.getFlashAttributes().get("message"));
        DraftView draft = ((List<DraftView>) redirect.getFlashAttributes().get("drafts")).get(0);
        assertEquals("Alpina", draft.supplier());
        assertEquals(List.of("19 x Leche"), draft.lines());
        assertEquals(List.of("21 x Avena por $ 50000"), draft.moqSuggestions());
    }

    /** Servicio falso: dos filas fijas, una que necesita pedido y otra que no, y un borrador. */
    static class FakeService implements ReplenishmentService {
        final List<ReorderStatus> rows = List.of(
                new ReorderStatus(1L, "1001", "Leche", "Alpina", 5, new BigDecimal("2.0000"), 10, true, 19),
                new ReorderStatus(2L, "1002", "Avena", "Alpina", 30, null, 4, false, 0));

        public List<ReorderStatus> reorderOverview() { return rows; }
        public List<Long> recalculateReorderPoints() { return List.of(1L, 2L); }
        public List<PurchaseOrder> generateDraftOrders() {
            return List.of(new PurchaseOrder(7L, 1L, OrderStatus.DRAFT, false, false, Instant.now(),
                    null, null, null, List.of(new PurchaseOrderLine(null, 7L, 1L, 19, 0, BigDecimal.TEN, false))));
        }
        public List<OrderLineSuggestion> suggestMoqCompletion(Long orderId) {
            return List.of(new OrderLineSuggestion(2L, 21, new BigDecimal("50000"), "rota rápido"));
        }
        public BudgetResult prioritizeUnderBudget(List<Long> ids, BigDecimal budget) { return null; }
    }
}
