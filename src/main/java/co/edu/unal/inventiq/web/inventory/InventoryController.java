package co.edu.unal.inventiq.web.inventory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import co.edu.unal.inventiq.application.sales.InventoryQueryService;

/** Vista de inventario del administrador (B5). Con A7 queda restringida al rol ADMIN. */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryQueryService inventory;

    public InventoryController(InventoryQueryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("overview", inventory.overview());
        return "inventory/list";
    }
}
