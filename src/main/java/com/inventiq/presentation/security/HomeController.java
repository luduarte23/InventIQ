package com.inventiq.presentation.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;

/**
 * Página de inicio: después del login todos llegan a "/" y de aquí cada rol va a su pantalla.
 * El administrador va a su inicio con las alertas (D3); el cajero, a la pantalla de ventas.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user != null && user.role() == Role.ADMIN) {
            return "redirect:/admin";
        }
        return "redirect:/pos";
    }
}