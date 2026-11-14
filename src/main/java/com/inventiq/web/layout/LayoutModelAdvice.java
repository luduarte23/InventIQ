package com.inventiq.web.layout;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;

/**
 * Datos que necesita el menú del layout (D1) en todas las pantallas: quién inició sesión
 * y si es administrador. Solo decide qué enlaces se muestran; quién puede entrar a cada
 * ruta lo sigue decidiendo SecurityConfig.
 */
@ControllerAdvice
public class LayoutModelAdvice {

    /** Nombre del usuario con sesión, o null en el login. */
    @ModelAttribute("username")
    public String username(@AuthenticationPrincipal AuthenticatedUser user) {
        return user == null ? null : user.username();
    }

    /** true si el usuario con sesión es ADMIN; así el menú muestra las pantallas de C y D. */
    @ModelAttribute("isAdmin")
    public boolean isAdmin(@AuthenticationPrincipal AuthenticatedUser user) {
        return user != null && user.role() == Role.ADMIN;
    }
}