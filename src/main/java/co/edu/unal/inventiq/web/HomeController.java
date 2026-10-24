package co.edu.unal.inventiq.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Por ahora el inicio es la pantalla de ventas. Con el login (A7) se redirige según el rol. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/pos";
    }
}
