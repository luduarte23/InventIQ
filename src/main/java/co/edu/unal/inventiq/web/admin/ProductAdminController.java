package co.edu.unal.inventiq.web.admin;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import co.edu.unal.inventiq.application.admin.BarcodeLookup;
import co.edu.unal.inventiq.application.admin.CatalogService;
import co.edu.unal.inventiq.application.sales.InventoryQueryService;
import co.edu.unal.inventiq.domain.exception.InvalidProductException;
import co.edu.unal.inventiq.domain.exception.ProductNotFoundException;
import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;

/**
 * Administración → Productos.
 * <ul>
 *   <li>Registrar en dos pasos: escanear o escribir el código (GET /new) y llenar el formulario (POST).</li>
 *   <li>Por fila: editar, pausar o habilitar la venta, ver los lotes y ver el historial de precios.</li>
 * </ul>
 * Con el login (A7) queda restringido al rol ADMIN.
 */
@Controller
@RequestMapping("/admin/products")
public class ProductAdminController {

    private final CatalogService catalog;
    private final InventoryQueryService inventory;

    public ProductAdminController(CatalogService catalog, InventoryQueryService inventory) {
        this.catalog = catalog;
        this.inventory = inventory;
    }

    @GetMapping
    public String list(Model model) {
        return catalogPage(model);
    }

    @GetMapping("/new")
    public String startRegistration(@RequestParam(defaultValue = "") String barcode, Model model) {
        if (barcode.isBlank()) {
            model.addAttribute("scanError", "Escanea o escribe un código para continuar.");
            return catalogPage(model);
        }
        return switch (catalog.lookup(barcode)) {
            case BarcodeLookup.Invalid invalid -> {
                model.addAttribute("scanError", invalid.reason());
                model.addAttribute("scannedBarcode", invalid.barcode());
                yield catalogPage(model);
            }
            case BarcodeLookup.AlreadyRegistered registered -> {
                model.addAttribute("existing", registered.entry());
                model.addAttribute("highlight", registered.entry().product().barcode());
                yield catalogPage(model);
            }
            case BarcodeLookup.Available available -> {
                ProductForm form = new ProductForm();
                form.setBarcode(available.barcode());
                yield formPage(form, null, model);
            }
        };
    }

    @PostMapping
    public String register(@ModelAttribute("product") ProductForm form, BindingResult binding, Model model,
                           RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            // Algún campo no se pudo leer (p. ej. letras en el precio): no se guarda nada,
            // pero se muestran también los demás errores para corregirlos de una vez.
            addFieldErrors(binding, catalog.validate(form.toNewProduct()));
            return formPage(form, null, model);
        }
        try {
            Product saved = catalog.register(form.toNewProduct());
            redirect.addFlashAttribute("success", registeredMessage(saved, form.getInitialQuantity()));
            redirect.addFlashAttribute("highlight", saved.barcode());
            return "redirect:/admin/products";
        } catch (InvalidProductException e) {
            addFieldErrors(binding, e.getFieldErrors());
            return formPage(form, null, model);
        }
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable long id, Model model, RedirectAttributes redirect) {
        try {
            Product product = catalog.get(id);
            return formPage(ProductForm.from(product), product, model);
        } catch (ProductNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @ModelAttribute("product") ProductForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        Product current;
        try {
            current = catalog.get(id);
        } catch (ProductNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products";
        }
        form.setBarcode(current.barcode());
        if (binding.hasErrors()) {
            addFieldErrors(binding, catalog.validateChanges(form.toProductData()));
            return formPage(form, current, model);
        }
        try {
            Product saved = catalog.update(id, form.toProductData());
            String message = "Guardaste los cambios de «" + saved.name() + "».";
            if (saved.pricesDifferFrom(current)) {
                message += " El nuevo precio quedó en el historial.";
            }
            redirect.addFlashAttribute("success", message);
            redirect.addFlashAttribute("highlight", saved.barcode());
            return "redirect:/admin/products";
        } catch (InvalidProductException e) {
            addFieldErrors(binding, e.getFieldErrors());
            return formPage(form, current, model);
        }
    }

    /** Pausar o habilitar la venta. El producto sigue en el inventario en ambos casos. */
    @PostMapping("/{id}/sale")
    public String toggleSale(@PathVariable long id, @RequestParam boolean enabled, RedirectAttributes redirect) {
        try {
            Product product = catalog.setSaleEnabled(id, enabled);
            redirect.addFlashAttribute("success", enabled
                    ? "«" + product.name() + "» ya se puede vender."
                    : "Pausaste la venta de «" + product.name() + "». Sigue en el inventario con su stock,"
                    + " pero el POS no lo venderá hasta que la habilites.");
            redirect.addFlashAttribute("highlight", product.barcode());
        } catch (ProductNotFoundException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/products";
    }

    private String catalogPage(Model model) {
        model.addAttribute("overview", catalog.overview());
        model.addAttribute("stock", inventory.overview().byProductId());
        model.addAttribute("categories", ProductCategory.values());
        return "admin/products";
    }

    /** @param editing el producto que se edita, o null si es uno nuevo */
    private String formPage(ProductForm form, Product editing, Model model) {
        model.addAttribute("product", form);
        model.addAttribute("editing", editing);
        model.addAttribute("suppliers", catalog.activeSuppliers());
        model.addAttribute("categories", ProductCategory.values());
        model.addAttribute("units", ContentUnit.values());
        return "admin/product-form";
    }

    /** No repite un error en un campo que ya tiene uno (p. ej. "escribe un número" y "escribe el precio"). */
    private static void addFieldErrors(BindingResult binding, Map<String, String> errors) {
        errors.forEach((field, message) -> {
            if (!binding.hasFieldErrors(field)) {
                binding.rejectValue(field, "invalid", message);
            }
        });
    }

    private static String registeredMessage(Product product, Integer initialQuantity) {
        String message = "Producto «" + product.name() + "» registrado";
        message += initialQuantity == null || initialQuantity <= 0 ? " sin stock inicial"
                : " con " + initialQuantity + (initialQuantity == 1 ? " unidad" : " unidades");
        return message + (product.saleEnabled() ? "." : ", con la venta pausada.");
    }
}
