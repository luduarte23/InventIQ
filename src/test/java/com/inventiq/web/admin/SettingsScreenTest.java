package com.inventiq.web.admin;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.support.TestUsers;

/**
 * Pantalla de parámetros (FR-08) de punta a punta. Usa el usuario "admin" real porque la
 * auditoría guarda quién hizo el cambio y la tabla exige que ese usuario exista.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SettingsScreenTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository users;

    private RequestPostProcessor realAdmin() {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        return TestUsers.as(new AuthenticatedUser(id, "admin", Role.ADMIN));
    }

    @Test
    void theFormShowsTheCurrentSettings() throws Exception {
        mockMvc.perform(get("/settings").with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/form"));
    }

    @Test
    void validSettingsAreSaved() throws Exception {
        mockMvc.perform(post("/settings").with(realAdmin()).with(csrf())
                        .param("weeklyBudget", "750000")
                        .param("salesWindowDays", "30")
                        .param("reviewPeriodDays", "7")
                        .param("expiryReviewHorizonDays", "30")
                        .param("adminWhatsappNumber", "+573001234567"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));

        mockMvc.perform(get("/settings").with(TestUsers.admin()))
                .andExpect(content().string(containsString("750000")));
    }

    @Test
    void invalidSettingsShowTheFormAgainWithTheError() throws Exception {
        mockMvc.perform(post("/settings").with(realAdmin()).with(csrf())
                        .param("weeklyBudget", "750000")
                        .param("salesWindowDays", "0")
                        .param("reviewPeriodDays", "7")
                        .param("expiryReviewHorizonDays", "30")
                        .param("adminWhatsappNumber", "+573001234567"))
                .andExpect(status().isOk())
                .andExpect(view().name("settings/form"))
                .andExpect(content().string(containsString("salesWindowDays debe ser mayor que 0")));
    }
}