package com.inventiq.presentation.security;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de inicio: después del login todos llegan a "/" y de ahí pasan a la pantalla de ventas.
 * Cuando existan las pantallas del administrador (C y D), aquí se decidirá a dónde va cada rol.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/pos";
    }
}