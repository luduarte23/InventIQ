package co.edu.unal.inventiq.web.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

/** Muestra la página de login. Validar la contraseña y cerrar sesión lo hace Spring Security. */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (request.getRemoteUser() != null) {
            return "redirect:/";
        }
        return "auth/login";
    }
}
