package com.inventiq.web.replenishment;

import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.application.replenishment.ReplenishmentService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantalla del motor de reabastecimiento: solo delega en el servicio y muestra el resultado. */
@Controller
public class ReplenishmentController {

    private final ReplenishmentService service;

    public ReplenishmentController(ReplenishmentService service) {
        this.service = service;
    }

    /** Estado de cada producto: stock, velocidad, punto de reorden y cuánto pedir. */
    @GetMapping("/replenishment")
    public String show(Model model) {
        List<ReorderStatus> rows = service.reorderOverview();
        model.addAttribute("rows", rows);
        model.addAttribute("toOrder", rows.stream().filter(ReorderStatus::needsReorder).count());
        return "replenishment/index";
    }

    /**
     * Botón "Generar borradores": recalcula los puntos de reorden, arma un borrador por proveedor
     * y, para cada uno, pide las sugerencias para llegar al pedido mínimo. Termina en redirect
     * para que recargar la página no repita la acción.
     */
    @PostMapping("/replenishment/drafts")
    public String generateDrafts(RedirectAttributes redirect) {
        int updated = service.recalculateReorderPoints().size();
        Map<Long, ReorderStatus> rows = service.reorderOverview().stream()
                .collect(Collectors.toMap(ReorderStatus::productId, row -> row));
        List<DraftView> drafts = service.generateDraftOrders().stream()
                .map(order -> DraftView.of(order, rows, service.suggestMoqCompletion(order.getId())))
                .toList();
        redirect.addFlashAttribute("message", updated + " puntos de reorden actualizados y "
                + drafts.size() + " borradores creados o ampliados.");
        redirect.addFlashAttribute("drafts", drafts);
        return "redirect:/replenishment";
    }
}
