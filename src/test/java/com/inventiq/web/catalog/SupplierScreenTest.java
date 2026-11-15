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

/** Pantalla de proveedores (D4) de punta a punta. @Transactional deshace lo que cada prueba crea. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SupplierScreenTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theListAndTheEmptyFormOpen() throws Exception {
        mockMvc.perform(get("/suppliers").with(TestUsers.admin())).andExpect(status().isOk());
        mockMvc.perform(get("/suppliers/new").with(TestUsers.admin())).andExpect(status().isOk());
    }

    @Test
    void aSupplierIsCreatedAndListed() throws Exception {
        mockMvc.perform(post("/suppliers").with(TestUsers.admin()).with(csrf())
                        .param("name", "Proveedor de prueba D4")
                        .param("whatsappNumber", "+573001112233")
                        .param("leadTimeDays", "2")
                        .param("minOrderAmount", "")
                        .param("minOrderUnits", "12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/suppliers"));

        mockMvc.perform(get("/suppliers").with(TestUsers.admin()))
                .andExpect(content().string(containsString("Proveedor de prueba D4")));
    }

    @Test
    void aSupplierWithoutNameShowsTheFormAgainWithTheError() throws Exception {
        mockMvc.perform(post("/suppliers").with(TestUsers.admin()).with(csrf())
                        .param("name", "")
                        .param("whatsappNumber", "+573001112233")
                        .param("leadTimeDays", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("suppliers/form"));
    }
}