package co.edu.unal.inventiq.web.pos;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.unal.inventiq.application.sales.SalesService;
import co.edu.unal.inventiq.domain.exception.BusinessException;
import co.edu.unal.inventiq.domain.model.Cart;
import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.web.Money;
import jakarta.servlet.http.HttpSession;

/**
 * Pantalla de ventas (B6). La venta en curso vive en la sesión del cajero. Todas las
 * acciones redirigen a GET /pos, así recargar la página nunca repite un escaneo, y un
 * error se muestra como mensaje sin perder lo que ya se escaneó.
 */
@Controller
@RequestMapping("/pos")
public class PosController {

    static final String CART_ATTRIBUTE = "posCart";

    /** Hasta que exista el login (A7) las ventas quedan a nombre del cajero inicial. */
    static final String DEFAULT_CASHIER = "cajero";

    private final SalesService sales;

    public PosController(SalesService sales) {
        this.sales = sales;
    }

    @GetMapping
    public String show(HttpSession session, Model model) {
        Cart cart = cart(session);
        model.addAttribute("cart", cart);
        model.addAttribute("total", Money.format(cart.total()));
        return "pos/sale";
    }

    @PostMapping("/scan")
    public String scan(@RequestParam(defaultValue = "") String barcode,
                       @RequestParam(defaultValue = "1") String quantity,
                       HttpSession session, RedirectAttributes redirect) {
        if (barcode.isBlank()) {
            return "redirect:/pos";
        }
        try {
            Cart cart = sales.scan(cart(session), barcode, parseQuantity(quantity));
            session.setAttribute(CART_ATTRIBUTE, cart);
            redirect.addFlashAttribute("lastScanned", barcode.strip());
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (NumberFormatException e) {
            redirect.addFlashAttribute("error", "La cantidad debe ser un número entero.");
        }
        return "redirect:/pos";
    }

    @PostMapping("/remove/{productId}")
    public String remove(@PathVariable long productId, HttpSession session) {
        Cart cart = cart(session);
        cart.remove(productId);
        session.setAttribute(CART_ATTRIBUTE, cart);
        return "redirect:/pos";
    }

    @PostMapping("/close")
    public String close(HttpSession session, Principal principal, RedirectAttributes redirect) {
        try {
            Sale sale = sales.close(cart(session), cashier(principal));
            session.removeAttribute(CART_ATTRIBUTE);
            int units = sale.unitCount();
            redirect.addFlashAttribute("success", "Venta registrada: " + units
                    + (units == 1 ? " unidad" : " unidades") + ", total " + Money.format(sale.total()) + ".");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pos";
    }

    @PostMapping("/cancel")
    public String cancel(HttpSession session, RedirectAttributes redirect) {
        session.removeAttribute(CART_ATTRIBUTE);
        redirect.addFlashAttribute("success", "Venta cancelada. El inventario no cambió.");
        return "redirect:/pos";
    }

    private Cart cart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute(CART_ATTRIBUTE);
        if (cart == null) {
            cart = sales.startSale();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
        return cart;
    }

    private static int parseQuantity(String quantity) {
        return quantity.isBlank() ? 1 : Integer.parseInt(quantity.strip());
    }

    private static String cashier(Principal principal) {
        return principal != null ? principal.getName() : DEFAULT_CASHIER;
    }
}
