package com.inventiq.web.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventiq.application.purchasing.PurchasingService;
import com.inventiq.application.security.AuthenticatedUser;

/** Pantalla de parámetros operativos (FR-08). Guardar y auditar lo hace PurchasingService. */
@Controller
public class SettingsController {

    private final PurchasingService purchasing;

    public SettingsController(PurchasingService purchasing) {
        this.purchasing = purchasing;
    }

    @GetMapping("/settings")
    public String show(Model model) {
        model.addAttribute("form", SettingsForm.from(purchasing.getSettings()));
        return "settings/form";
    }

    @PostMapping("/settings")
    public String save(@ModelAttribute("form") SettingsForm form, BindingResult binding,
                       @AuthenticationPrincipal AuthenticatedUser user, Model model,
                       RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("error", "Revisa los campos: hay valores que no son números válidos.");
            return "settings/form";
        }
        try {
            purchasing.updateSettings(form.toSettings(), user.userId());
            redirect.addFlashAttribute("message", "Parámetros guardados.");
            return "redirect:/settings";
        } catch (IllegalArgumentException | NullPointerException e) {
            model.addAttribute("error", e.getMessage());
            return "settings/form";
        }
    }
}