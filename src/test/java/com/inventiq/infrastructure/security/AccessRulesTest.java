package com.inventiq.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.support.TestUsers;

/**
 * Reglas de acceso de SecurityConfig (NFR-03), probadas con peticiones simuladas:
 * sin sesión todo lleva al login; el cajero solo usa /pos; el administrador entra a todo.
 *
 * @Transactional deshace al final de cada prueba cualquier cambio que una pantalla haga en la base.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccessRulesTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------- Sin sesión ----------

    @ParameterizedTest
    @ValueSource(strings = {"/", "/admin", "/pos", "/replenishment", "/h2-console", "/ruta-que-no-existe"})
    void withoutSessionEveryPageRedirectsToLogin(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectsToLogin());
    }

    @Test
    void theLoginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    // ---------- Cajero ----------

    @Test
    void cashierCanOpenTheSalesScreen() throws Exception {
        mockMvc.perform(get("/pos").with(TestUsers.cashier()))
                .andExpect(status().isOk())
                .andExpect(view().name("pos/sale"));
    }

    @Test
    void homeSendsALoggedInUserToTheSalesScreen() throws Exception {
        mockMvc.perform(get("/").with(TestUsers.cashier()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pos"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/replenishment", "/receiving", "/stock", "/settings", "/suppliers", "/products", "/admin", "/h2-console", "/ruta-que-no-existe"})
    void cashierIsForbiddenEverywhereElse(String url) throws Exception {
        mockMvc.perform(get(url).with(TestUsers.cashier()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotCreateReplenishmentDrafts() throws Exception {
        mockMvc.perform(post("/replenishment/drafts").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotResolveAlerts() throws Exception {
        mockMvc.perform(post("/admin/alerts/1/resolve").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotChangeProducts() throws Exception {
        mockMvc.perform(post("/products/1/active").param("active", "false")
                        .with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotChangeSuppliers() throws Exception {
        mockMvc.perform(post("/suppliers").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotChangeSettings() throws Exception {
        mockMvc.perform(post("/settings").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotAdjustStock() throws Exception {
        mockMvc.perform(post("/stock/1/batches/1").param("newQuantity", "0").param("reason", "x")
                        .with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cashierCannotReceiveOrders() throws Exception {
        mockMvc.perform(post("/receiving/1").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/receiving/1/close").with(TestUsers.cashier()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    // ---------- Administrador ----------

    
    
    @Test
    void adminCanOpenReceiving() throws Exception {
        mockMvc.perform(get("/receiving").with(TestUsers.admin())).andExpect(status().isOk());
    }
    
    @Test
    void adminCanOpenSettings() throws Exception {
        mockMvc.perform(get("/settings").with(TestUsers.admin())).andExpect(status().isOk());
    }

    @Test
    void adminCanOpenStock() throws Exception {
        mockMvc.perform(get("/stock").with(TestUsers.admin())).andExpect(status().isOk());
    }

    @Test
    void adminCanOpenProducts() throws Exception {
        mockMvc.perform(get("/products").with(TestUsers.admin())).andExpect(status().isOk());
    }
    
    @Test
    void adminCanOpenSuppliers() throws Exception {
        mockMvc.perform(get("/suppliers").with(TestUsers.admin())).andExpect(status().isOk());
    }
    
    @Test
    void homeSendsTheAdminToTheAdminHome() throws Exception {
        mockMvc.perform(get("/").with(TestUsers.admin()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void adminCanOpenTheAdminHome() throws Exception {
        mockMvc.perform(get("/admin").with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/home"));
    }

    @Test
    void adminCanResolveAnAlert() throws Exception {
        // La alerta 999 no existe: el controlador muestra el error y vuelve al inicio.
        mockMvc.perform(post("/admin/alerts/999/resolve").with(TestUsers.admin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
    
    @Test
    void adminCanOpenTheSalesScreen() throws Exception {
        mockMvc.perform(get("/pos").with(TestUsers.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanOpenReplenishment() throws Exception {
        mockMvc.perform(get("/replenishment").with(TestUsers.admin()))
                .andExpect(status().isOk());
    }

    @Test
    void adminPassesSecurityOnUnknownPages() throws Exception {
        // 404 y no 403: la seguridad lo dejó pasar; simplemente la página no existe.
        mockMvc.perform(get("/ruta-que-no-existe").with(TestUsers.admin()))
                .andExpect(status().isNotFound());
    }

    /** La respuesta es una redirección a /login (con o sin "http://localhost" delante). */
    private static ResultMatcher redirectsToLogin() {
        return result -> {
            String location = result.getResponse().getRedirectedUrl();
            assertNotNull(location, "Se esperaba una redirección al login");
            assertTrue(location.endsWith("/login"), "Redirigió a " + location + " y no al login");
        };
    }
}