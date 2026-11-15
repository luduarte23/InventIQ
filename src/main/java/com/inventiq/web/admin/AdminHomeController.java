package com.inventiq.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
}