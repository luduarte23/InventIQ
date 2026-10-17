package co.edu.unal.inventiq.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import co.edu.unal.inventiq.domain.model.Role;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Datos que usa la plantilla base en todas las páginas: la ruta actual (para marcar el ítem
 * activo de la barra lateral), la fecha de hoy (cabecera) y quién inició sesión (menú por rol y
 * botón de salir). Thymeleaf 3.1 ya no expone el request a las plantillas, por eso se pasa en el
 * modelo.
 */
@ControllerAdvice
public class NavigationAdvice {

    /** Nombre del rol tal como lo ve Spring Security (sin el prefijo ROLE_). */
    public static final String ADMIN_ROLE = Role.ADMIN.name();

    private final Clock clock;

    public NavigationAdvice(Clock clock) {
        this.clock = clock;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("today")
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Usuario con sesión, o null en las páginas públicas. */
    @ModelAttribute("currentUser")
    public String currentUser(HttpServletRequest request) {
        return request.getRemoteUser();
    }

    /** El menú de administración solo se muestra al administrador. */
    @ModelAttribute("isAdmin")
    public boolean isAdmin(HttpServletRequest request) {
        return request.isUserInRole(ADMIN_ROLE);
    }

    @ModelAttribute("currentRoleLabel")
    public String currentRoleLabel(HttpServletRequest request) {
        if (request.getRemoteUser() == null) {
            return null;
        }
        return (request.isUserInRole(ADMIN_ROLE) ? Role.ADMIN : Role.CASHIER).label();
    }
}
