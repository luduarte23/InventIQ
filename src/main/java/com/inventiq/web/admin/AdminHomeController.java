package com.inventiq.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventiq.application.admin.AlertInboxService;

/** Inicio del administrador (D3): sus alertas sin resolver. Solo delega en el servicio. */
@Controller
public class AdminHomeController {

    private final AlertInboxService alerts;

    public AdminHomeController(AlertInboxService alerts) {
        this.alerts = alerts;
    }

    @GetMapping("/admin")
    public String home(Model model) {
        model.addAttribute("alerts", alerts.unresolvedAlerts());
        return "admin/home";
    }
    
    /** Botón "Resolver". Termina en redirect para que recargar la página no repita la acción. */
    @PostMapping("/admin/alerts/{id}/resolve")
    public String resolve(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            alerts.resolve(id);
            redirect.addFlashAttribute("message", "Alerta marcada como resuelta.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }
}