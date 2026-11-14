package com.inventiq.web.replenishment;

import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.application.replenishment.ReplenishmentService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}
