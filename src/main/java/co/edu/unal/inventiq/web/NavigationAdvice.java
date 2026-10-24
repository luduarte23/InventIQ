package co.edu.unal.inventiq.web;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Datos que usa la plantilla base en todas las páginas: la ruta actual (para marcar el ítem
 * activo de la barra lateral) y la fecha de hoy (cabecera). Thymeleaf 3.1 ya no expone el
 * request a las plantillas, por eso se pasa en el modelo.
 */
@ControllerAdvice
public class NavigationAdvice {

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
}
