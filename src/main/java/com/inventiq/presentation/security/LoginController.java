package com.inventiq.presentation.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Muestra la página de login (FR-01). Solo atiende el GET: el POST de /login (verificar usuario
 * y contraseña) y el /logout los procesa Spring Security, según SecurityConfig.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
