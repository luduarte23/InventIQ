package co.edu.unal.inventiq.web.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * A7: login con los usuarios reales de la migración (admin / admin123, cajero / cajero123),
 * redirección por rol y 403 del cajero en las rutas de administración. Las pruebas de seguridad
 * exhaustivas (todas las rutas, sin sesión) son de A8.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:login;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class LoginAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theLoginPageIsPublicAndCarriesTheCsrfToken() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Iniciar sesión")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void withoutASessionEveryScreenSendsYouToTheLogin() throws Exception {
        mvc.perform(get("/pos")).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/admin/products")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void theAdministratorLogsInAndLandsOnTheHomePage() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ventas de hoy")))
                .andExpect(content().string(containsString("href=\"/admin/products\"")))
                .andExpect(content().string(containsString("Administrador")));
    }

    @Test
    void theCashierLogsInAndIsSentToSales() throws Exception {
        MockHttpSession session = login("cajero", "cajero123");

        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/pos"));
        mvc.perform(get("/pos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cajero")))
                // El cajero solo ve Ventas en el menú.
                .andExpect(content().string(not(containsString("href=\"/inventory\""))))
                .andExpect(content().string(not(containsString("href=\"/admin/products\""))));
    }

    @Test
    void theCashierGetsA403OnAdministrationScreens() throws Exception {
        MockHttpSession session = login("cajero", "cajero123");

        for (String adminPath : new String[] {"/inventory", "/admin/products", "/admin/history"}) {
            mvc.perform(get(adminPath).session(session)).andExpect(status().isForbidden());
        }
    }

    @Test
    void theForbiddenPageExplainsItAndOffersSales() throws Exception {
        MockHttpSession session = login("cajero", "cajero123");

        mvc.perform(get("/error").session(session).accept(MediaType.TEXT_HTML)
                        .requestAttr("jakarta.servlet.error.status_code", 403))
                .andExpect(content().string(containsString("No tienes permiso para ver esta página")))
                .andExpect(content().string(containsString("Ir a Ventas")));
    }

    @Test
    void aWrongPasswordIsRejected() throws Exception {
        mvc.perform(formLogin().user("admin").password("incorrecta"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());

        mvc.perform(get("/login").param("error", ""))
                .andExpect(content().string(containsString("Usuario o contraseña incorrectos.")));
    }

    @Test
    void anInactiveUserCannotLogInEvenWithTheRightPassword() throws Exception {
        jdbc.update("insert into app_user (username, password_hash, role, active) values (?, ?, ?, ?)",
                "exempleado", new BCryptPasswordEncoder().encode("clave123"), "CASHIER", false);

        mvc.perform(formLogin().user("exempleado").password("clave123"))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void loggingOutEndsTheSession() throws Exception {
        MockHttpSession session = login("admin", "admin123");

        mvc.perform(post("/logout").with(csrf()).session(session)).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/").session(session)).andExpect(redirectedUrl("/login"));
    }

    @Test
    void aLoggedInUserVisitingTheLoginPageGoesHome() throws Exception {
        mvc.perform(get("/login").session(login("admin", "admin123"))).andExpect(redirectedUrl("/"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mvc.perform(formLogin().user(username).password(password))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(username))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
