package com.inventiq.web.pos;

import com.inventiq.application.pos.SalesService;
import com.inventiq.application.pos.ScanResult;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.exception.DomainException;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Capa de presentación de la venta (B6). Solo recibe, delega en SalesService y responde:
 * no tiene reglas de negocio. Cada acción termina en "redirect:/pos", así recargar la
 * página nunca repite un escaneo.
 */
@Controller
public class PosController {

    /** Mientras no exista el login (A7), las ventas quedan a nombre del usuario con id 1. */
    private static final Long CASHIER_ID = 1L;

    /** Nombre con el que se guarda la venta en curso en la sesión del cajero. */
    private static final String CURRENT_SALE = "currentSale";

    private final SalesService salesService;

    public PosController(SalesService salesService) {
        this.salesService = salesService;
    }

    /** Muestra la pantalla de ventas con la venta en curso (si hay una). */
    @GetMapping("/pos")
    public String showSale(HttpSession session, Model model) {
        model.addAttribute("sale", session.getAttribute(CURRENT_SALE));
        return "pos/sale";
    }

    /** El cajero escaneó (o escribió) un código y presionó Enter. */
    @PostMapping("/pos/scan")
    public String scan(@ModelAttribute ScanForm form, BindingResult binding, HttpSession session,
                       RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            redirect.addFlashAttribute("error", "La cantidad debe ser un número entero.");
            return "redirect:/pos";
        }
        try {
            SaleScreen sale = currentSaleOrStartNew(session);
            ScanResult result = salesService.scan(sale.getSaleId(), form.getBarcode(), form.getQuantity());
            sale.addScan(result);
        } catch (DomainException e) {
            // El error no corta la venta: se muestra el mensaje y lo ya escaneado sigue ahí.
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pos";
    }

    @PostMapping("/pos/finish")
    public String finish(HttpSession session, RedirectAttributes redirect) {
        SaleScreen sale = (SaleScreen) session.getAttribute(CURRENT_SALE);
        if (sale == null) {
            redirect.addFlashAttribute("error", "No hay una venta en curso.");
            return "redirect:/pos";
        }
        try {
            Sale finished = salesService.finishSale(sale.getSaleId());
            session.removeAttribute(CURRENT_SALE);
            redirect.addFlashAttribute("message", "Venta registrada. Total: $ " + finished.getTotal());
        } catch (DomainException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pos";
    }

    @PostMapping("/pos/cancel")
    public String cancel(HttpSession session, RedirectAttributes redirect) {
        SaleScreen sale = (SaleScreen) session.getAttribute(CURRENT_SALE);
        if (sale == null) {
            redirect.addFlashAttribute("error", "No hay una venta en curso.");
            return "redirect:/pos";
        }
        try {
            salesService.cancelSale(sale.getSaleId());
            session.removeAttribute(CURRENT_SALE);
            redirect.addFlashAttribute("message", "Venta cancelada. Las unidades volvieron al inventario.");
        } catch (DomainException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/pos";
    }

    /** Devuelve la venta en curso; si no hay, le pide al servicio que abra una nueva. */
    private SaleScreen currentSaleOrStartNew(HttpSession session) {
        SaleScreen sale = (SaleScreen) session.getAttribute(CURRENT_SALE);
        if (sale == null) {
            Sale started = salesService.startSale(CASHIER_ID);
            sale = new SaleScreen(started.getId());
            session.setAttribute(CURRENT_SALE, sale);
        }
        return sale;
    }
}
