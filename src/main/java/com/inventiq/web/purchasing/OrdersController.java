package com.inventiq.web.purchasing;

import java.util.ArrayList;
import java.util.List;
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
import com.inventiq.application.purchasing.OrderLineEdit;
import com.inventiq.application.purchasing.PurchasingService;
import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.Supplier;

/**
 * Pantalla de pedidos (D7): lista, editar un borrador, aprobar (NFR-02: Inicio -> Pedidos ->
 * Aprobar) y enviar al proveedor (FR-09). Las reglas están en PurchasingService.
 */
@Controller
public class OrdersController {

    private final PurchasingService purchasing;
    private final CatalogService catalog;

    public OrdersController(PurchasingService purchasing, CatalogService catalog) {
        this.purchasing = purchasing;
        this.catalog = catalog;
    }

    @GetMapping("/orders")
    public String list(Model model) {
        model.addAttribute("orders", purchasing.allOrders());
        model.addAttribute("supplierNames", catalog.allSuppliers().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName)));
        return "orders/list";
    }

    @GetMapping("/orders/{orderId}")
    public String detail(@PathVariable Long orderId, Model model) {
        PurchaseOrder order = purchasing.getOrder(orderId);
        model.addAttribute("order", order);
        model.addAttribute("productNames", catalog.allProducts().stream()
                .collect(Collectors.toMap(Product::getId, Product::getName)));
        model.addAttribute("supplierNames", catalog.allSuppliers().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName)));
        if (order.getSupplierId() != null && order.getStatus() != OrderStatus.DRAFT) {
            model.addAttribute("message", purchasing.buildSupplierMessage(orderId));
        }
        return "orders/detail";
    }

    /** Guardar cantidades de un borrador. 0 quita la línea. */
    @PostMapping("/orders/{orderId}/lines")
    public String updateLines(@PathVariable Long orderId, @RequestParam List<Long> productId,
                              @RequestParam List<Integer> quantity,
                              @RequestParam List<Boolean> addedByMoq, RedirectAttributes redirect) {
        try {
            List<OrderLineEdit> edits = new ArrayList<>();
            for (int i = 0; i < productId.size(); i++) {
                edits.add(new OrderLineEdit(productId.get(i), quantity.get(i), addedByMoq.get(i)));
            }
            purchasing.updateOrderLines(orderId, edits);
            redirect.addFlashAttribute("notice", "Cantidades guardadas.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }

    /** Botón "Aprobar", en la lista y en el detalle. Vuelve a donde estaba el admin. */
    @PostMapping("/orders/{orderId}/approve")
    public String approve(@PathVariable Long orderId,
                          @RequestParam(defaultValue = "/orders") String back,
                          @AuthenticationPrincipal AuthenticatedUser user, RedirectAttributes redirect) {
        try {
            purchasing.approveOrder(orderId, user.userId());
            redirect.addFlashAttribute("notice", "Pedido #" + orderId + " aprobado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + (back.startsWith("/orders") ? back : "/orders");
    }

    /** Botón "Enviar al proveedor". Si el WhatsApp falla queda una alerta en el inicio. */
    @PostMapping("/orders/{orderId}/send")
    public String send(@PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser user,
                       RedirectAttributes redirect) {
        try {
            purchasing.sendOrder(orderId, user.userId());
            redirect.addFlashAttribute("notice", "Pedido #" + orderId + " marcado como enviado.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + orderId;
    }
}