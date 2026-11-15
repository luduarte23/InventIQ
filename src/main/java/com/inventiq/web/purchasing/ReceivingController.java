package com.inventiq.web.purchasing;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventiq.application.catalog.CatalogService;
import com.inventiq.application.purchasing.PurchasingService;
import com.inventiq.application.purchasing.ReceiptLine;
import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Supplier;

/**
 * Pantalla de recepción de pedidos (FR-10, HU-05). Arma las líneas recibidas a partir del
 * formulario y deja todas las reglas a PurchasingService.confirmReceipt.
 */
@Controller
public class ReceivingController {

    private final PurchasingService purchasing;
    private final CatalogService catalog;

    public ReceivingController(PurchasingService purchasing, CatalogService catalog) {
        this.purchasing = purchasing;
        this.catalog = catalog;
    }

    /** Pedidos enviados o recibidos a medias. */
    @GetMapping("/receiving")
    public String list(Model model) {
        model.addAttribute("orders", purchasing.ordersAwaitingReception());
        model.addAttribute("supplierNames", catalog.allSuppliers().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName)));
        return "receiving/list";
    }

    /** Formulario de un pedido: una fila por producto con lo pendiente. */
    @GetMapping("/receiving/{orderId}")
    public String order(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", purchasing.getOrder(orderId));
        model.addAttribute("products", catalog.allProducts().stream()
                .collect(Collectors.toMap(Product::getId, Function.identity())));
        return "receiving/order";
    }

    /** Botón "Confirmar recepción". Las tres listas llegan en el mismo orden, una entrada por fila. */
    @PostMapping("/receiving/{orderId}")
    public String confirm(@PathVariable Long orderId,
                          @RequestParam List<Long> productId,
                          @RequestParam List<Integer> quantity,
                          @RequestParam List<String> expirationDate,
                          @AuthenticationPrincipal AuthenticatedUser user,
                          RedirectAttributes redirect) {
        try {
            List<ReceiptLine> lines = new ArrayList<>();
            for (int i = 0; i < productId.size(); i++) {
                lines.add(new ReceiptLine(productId.get(i), quantity.get(i), parseDate(expirationDate, i)));
            }
            PurchaseOrder order = purchasing.confirmReceipt(orderId, lines, user.userId());
            if (order.getStatus() == OrderStatus.RECEIVED) {
                redirect.addFlashAttribute("message", "Pedido " + orderId + " recibido completo.");
                return "redirect:/receiving";
            }
            redirect.addFlashAttribute("message", "Recepción registrada. El pedido sigue con pendientes.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/receiving/" + orderId;
    }

    /** Botón "Cerrar con pendientes" (HU-05 esc. 2). */
    @PostMapping("/receiving/{orderId}/close")
    public String close(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user,
                        RedirectAttributes redirect) {
        try {
            purchasing.closeOrder(orderId, user.userId());
            redirect.addFlashAttribute("message", "Pedido " + orderId + " cerrado con pendientes.");
            return "redirect:/receiving";
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/receiving/" + orderId;
        }
    }

    /** El campo de fecha vacío significa "sin vencimiento". */
    private static LocalDate parseDate(List<String> dates, int i) {
        if (i >= dates.size() || dates.get(i) == null || dates.get(i).isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dates.get(i));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Fecha de vencimiento inválida: " + dates.get(i));
        }
    }
}