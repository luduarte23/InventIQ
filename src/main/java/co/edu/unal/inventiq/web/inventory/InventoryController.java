package co.edu.unal.inventiq.web.inventory;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.unal.inventiq.application.sales.InventoryQueryService;
import co.edu.unal.inventiq.domain.model.ProductCategory;

/**
 * Vista de inventario del administrador (B5). Con A7 queda restringida al rol ADMIN.
 * {@code q} llega desde el botón "Lotes" de Productos y prellena el buscador.
 */
@Controller
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryQueryService inventory;

    public InventoryController(InventoryQueryService inventory) {
        this.inventory = inventory;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("overview", inventory.overview());
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("query", q.strip());
        return "inventory/list";
    }
}
