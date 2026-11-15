package com.inventiq.web.catalog;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.support.TestUsers;

/**
 * Pantalla de productos (D4) de punta a punta: controlador, servicio, plantilla y base H2.
 * @Transactional deshace lo que cada prueba crea.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theListAndTheEmptyFormOpen() throws Exception {
        mockMvc.perform(get("/products").with(TestUsers.admin())).andExpect(status().isOk());
        mockMvc.perform(get("/products/new").with(TestUsers.admin())).andExpect(status().isOk());
    }

    @Test
    void aProductIsCreatedAndListed() throws Exception {
        mockMvc.perform(post("/products").with(TestUsers.admin()).with(csrf())
                        .param("barcode", "9990001112223")
                        .param("name", "Producto de prueba D4")
                        .param("salePrice", "5000")
                        .param("unitCost", "3500")
                        .param("safetyStock", "3")
                        .param("supplierId", "")
                        .param("perishable", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(get("/products").with(TestUsers.admin()))
                .andExpect(content().string(containsString("Producto de prueba D4")));
    }

    @Test
    void anInvalidProductShowsTheFormAgainWithTheError() throws Exception {
        mockMvc.perform(post("/products").with(TestUsers.admin()).with(csrf())
                        .param("barcode", "9990001112224")
                        .param("name", "Sin precio")
                        .param("salePrice", "")
                        .param("unitCost", "100")
                        .param("safetyStock", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("products/form"))
                .andExpect(content().string(containsString("El precio de venta y el costo son obligatorios")));
    }
}