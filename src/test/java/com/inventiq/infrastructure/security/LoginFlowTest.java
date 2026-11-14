package com.inventiq.infrastructure.security;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.User;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * El login de punta a punta (FR-01): formulario -> Spring Security -> InventiqAuthenticationProvider
 * -> AuthenticationService -> base de datos, con los usuarios reales que crea InitialUsers.
 * También el cierre de sesión y la protección CSRF de los formularios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ---------- Login ----------

    @Test
    void cashierLogsInWithTheInitialPassword() throws Exception {
        mockMvc.perform(formLogin().user("cajero").password("cajero123"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles("CASHIER"))
                .andExpect(authenticated().withAuthentication(
                        auth -> assertInstanceOf(AuthenticatedUser.class, auth.getPrincipal())));
    }

    @Test
    void adminLogsInWithTheInitialPassword() throws Exception {
        mockMvc.perform(formLogin().user("admin").password("admin123"))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void wrongPasswordGoesBackToLoginWithError() throws Exception {
        mockMvc.perform(formLogin().user("cajero").password("no-es-la-clave"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void unknownUserGetsTheSameAnswerAsAWrongPassword() throws Exception {
        mockMvc.perform(formLogin().user("no-existe").password("cajero123"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void inactiveUserCannotLogInEvenWithTheRightPassword() throws Exception {
        users.save(new User(null, "retirado-a8", passwordEncoder.encode("clave123"), Role.CASHIER, false));

        mockMvc.perform(formLogin().user("retirado-a8").password("clave123"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    // ---------- La página de login ----------

    @Test
    void loginPageShowsTheErrorMessageAndCarriesTheCsrfToken() throws Exception {
        mockMvc.perform(get("/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginPageShowsTheLogoutMessage() throws Exception {
        mockMvc.perform(get("/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sesión cerrada")));
    }

    // ---------- Logout ----------

    @Test
    void logoutGoesBackToLoginWithTheLogoutMessage() throws Exception {
        mockMvc.perform(post("/logout").with(TestUsers.cashier()).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    // ---------- CSRF: un POST sin token se rechaza aunque el usuario tenga permiso ----------

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/login").param("username", "cajero").param("password", "cajero123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/logout").with(TestUsers.cashier()))
                .andExpect(status().isForbidden());
    }

    @Test
    void scanWithoutCsrfTokenIsRejectedEvenForTheCashier() throws Exception {
        mockMvc.perform(post("/pos/scan").with(TestUsers.cashier())
                        .param("barcode", "7700000000001").param("quantity", "1"))
                .andExpect(status().isForbidden());
    }
}