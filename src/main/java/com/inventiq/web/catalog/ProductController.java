package com.inventiq.web.catalog;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.inventiq.application.catalog.CatalogService;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Supplier;

/** Pantalla de productos (D4): lista, crear, editar e inactivar. Las reglas están en CatalogService. */
@Controller
public class ProductController {

    private final CatalogService catalog;

    public ProductController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/products")
    public String list(Model model) {
        Map<Long, String> supplierNames = catalog.allSuppliers().stream()
                .collect(Collectors.toMap(Supplier::getId, Supplier::getName));
        model.addAttribute("products", catalog.allProducts());
        model.addAttribute("supplierNames", supplierNames);
        return "products/list";
    }

    @GetMapping("/products/new")
    public String newForm(Model model) {
        return showForm(model, new ProductForm(), null);
    }

    @PostMapping("/products")
    public String create(@ModelAttribute("form") ProductForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return showForm(model, form, null, "Revisa los campos: hay valores que no son números válidos.");
        }
        try {
            Product created = catalog.createProduct(form.toData());
            redirect.addFlashAttribute("message", "Producto " + created.getName() + " creado.");
            return "redirect:/products";
        } catch (IllegalArgumentException | NullPointerException e) {
            return showForm(model, form, null, e.getMessage());
        }
    }

    @GetMapping("/products/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(model, ProductForm.from(catalog.getProduct(id)), id);
    }

    @PostMapping("/products/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") ProductForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return showForm(model, form, id, "Revisa los campos: hay valores que no son números válidos.");
        }
        try {
            Product updated = catalog.updateProduct(id, form.toData());
            redirect.addFlashAttribute("message", "Producto " + updated.getName() + " actualizado.");
            return "redirect:/products";
        } catch (IllegalArgumentException | NullPointerException e) {
            return showForm(model, form, id, e.getMessage());
        }
    }

    /** Botón Inactivar / Reactivar de la lista. */
    @PostMapping("/products/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active,
                            RedirectAttributes redirect) {
        catalog.setProductActive(id, active);
        redirect.addFlashAttribute("message", active ? "Producto reactivado." : "Producto inactivado.");
        return "redirect:/products";
    }

    private String showForm(Model model, ProductForm form, Long id) {
        return showForm(model, form, id, null);
    }

    /** id null = producto nuevo. Solo se ofrecen proveedores activos. */
    private String showForm(Model model, ProductForm form, Long id, String error) {
        model.addAttribute("form", form);
        model.addAttribute("productId", id);
        model.addAttribute("suppliers", catalog.activeSuppliers());
        model.addAttribute("error", error);
        return "products/form";
    }
}