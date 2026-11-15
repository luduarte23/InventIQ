package com.inventiq.web.catalog;

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
import com.inventiq.domain.model.Supplier;

/** Pantalla de proveedores (D4): lista, crear, editar e inactivar. Las reglas están en CatalogService. */
@Controller
public class SupplierController {

    private final CatalogService catalog;

    public SupplierController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/suppliers")
    public String list(Model model) {
        model.addAttribute("suppliers", catalog.allSuppliers());
        return "suppliers/list";
    }

    @GetMapping("/suppliers/new")
    public String newForm(Model model) {
        return showForm(model, new SupplierForm(), null, null);
    }

    @PostMapping("/suppliers")
    public String create(@ModelAttribute("form") SupplierForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return showForm(model, form, null, "Revisa los campos: hay valores que no son números válidos.");
        }
        try {
            Supplier created = catalog.createSupplier(form.toData());
            redirect.addFlashAttribute("message", "Proveedor " + created.getName() + " creado.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException e) {
            return showForm(model, form, null, e.getMessage());
        }
    }

    @GetMapping("/suppliers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(model, SupplierForm.from(catalog.getSupplier(id)), id, null);
    }

    @PostMapping("/suppliers/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") SupplierForm form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            return showForm(model, form, id, "Revisa los campos: hay valores que no son números válidos.");
        }
        try {
            Supplier updated = catalog.updateSupplier(id, form.toData());
            redirect.addFlashAttribute("message", "Proveedor " + updated.getName() + " actualizado.");
            return "redirect:/suppliers";
        } catch (IllegalArgumentException e) {
            return showForm(model, form, id, e.getMessage());
        }
    }

    @PostMapping("/suppliers/{id}/active")
    public String setActive(@PathVariable Long id, @RequestParam boolean active,
                            RedirectAttributes redirect) {
        catalog.setSupplierActive(id, active);
        redirect.addFlashAttribute("message", active ? "Proveedor reactivado." : "Proveedor inactivado.");
        return "redirect:/suppliers";
    }

    /** id null = proveedor nuevo. */
    private String showForm(Model model, SupplierForm form, Long id, String error) {
        model.addAttribute("form", form);
        model.addAttribute("supplierId", id);
        model.addAttribute("error", error);
        return "suppliers/form";
    }
}