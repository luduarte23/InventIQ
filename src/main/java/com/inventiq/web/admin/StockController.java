package com.inventiq.web.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventiq.application.purchasing.PurchasingService;
import com.inventiq.application.replenishment.ReorderStatus;
import com.inventiq.application.replenishment.ReplenishmentService;
import com.inventiq.application.security.AuthenticatedUser;

/**
 * Ajuste manual de stock (FR-08). La lista de productos con su stock la da el motor de C
 * (reorderOverview); los lotes y el ajuste, PurchasingService.
 */
@Controller
public class StockController {

    private final PurchasingService purchasing;
    private final ReplenishmentService replenishment;

    public StockController(PurchasingService purchasing, ReplenishmentService replenishment) {
        this.purchasing = purchasing;
        this.replenishment = replenishment;
    }

    @GetMapping("/stock")
    public String list(Model model) {
        model.addAttribute("rows", replenishment.reorderOverview());
        return "stock/list";
    }

    @GetMapping("/stock/{productId}")
    public String product(@PathVariable Long productId, Model model) {
        ReorderStatus product = replenishment.reorderOverview().stream()
                .filter(row -> row.productId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("El producto " + productId + " no existe"));
        model.addAttribute("product", product);
        model.addAttribute("batches", purchasing.batchesOf(productId));
        return "stock/product";
    }

    @PostMapping("/stock/{productId}/batches/{batchId}")
    public String adjust(@PathVariable Long productId, @PathVariable Long batchId,
                         @RequestParam int newQuantity, @RequestParam String reason,
                         @AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        try {
            purchasing.adjustStock(productId, batchId, newQuantity, reason, user.userId());
            redirect.addFlashAttribute("message", "Stock del lote " + batchId + " ajustado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/stock/" + productId;
    }
}