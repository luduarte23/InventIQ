package co.edu.unal.inventiq.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * A8 (NFR-03): barrido de seguridad sobre <b>todas</b> las rutas de la aplicación.
 *
 * <p>Las rutas no se escriben a mano: se leen de Spring MVC. Así, cuando C o D agreguen una pantalla,
 * esta prueba la revisa sin que nadie tenga que acordarse. Si una pantalla nueva debe verla el cajero,
 * hay que abrirla en {@code SecurityConfig} y agregarla a {@link #isCashierRoute}.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:security-routes;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SecurityRoutesTest {

    /** Rutas que cualquiera puede abrir sin sesión. */
    private static final Set<String> PUBLIC_ROUTES = Set.of("/login", "/error");

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor CASHIER = user("cajero").roles("CASHIER");

    @Autowired
    private MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;

    private List<Route> routes;

    @BeforeEach
    void readRoutesFromSpring() {
        routes = new ArrayList<>();
        for (RequestMappingInfo info : mappings.getHandlerMethods().keySet()) {
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : info.getPathPatternsCondition().getPatternValues()) {
                if (methods.isEmpty()) {
                    routes.add(new Route(HttpMethod.GET, pattern));
                }
                methods.forEach(method -> routes.add(new Route(HttpMethod.valueOf(method.name()), pattern)));
            }
        }
    }

    @Test
    void theSweepFindsEveryKnownScreen() {
        assertThat(routes).extracting(Route::toString).contains(
                "GET /", "GET /login", "GET /pos", "POST /pos/close", "GET /inventory",
                "GET /admin/products", "POST /admin/products", "POST /admin/products/{id}/sale",
                "GET /admin/history");
    }

    @Test
    void withoutASessionOnlyTheLoginAndErrorPagesOpen() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Route route : protectedRoutes()) {
            MockHttpServletResponse response = perform(route, null, true);
            if (response.getStatus() != 302 || !"/login".equals(response.getRedirectedUrl())) {
                wrong.add(route + " -> " + response.getStatus() + " " + response.getRedirectedUrl());
            }
        }
        assertThat(wrong).as("rutas que no mandan al login sin sesión").isEmpty();
    }

    @Test
    void theCashierGetsA403OnEveryAdministrationRoute() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Route route : adminRoutes()) {
            int status = perform(route, CASHIER, true).getStatus();
            if (status != 403) {
                wrong.add(route + " -> " + status);
            }
        }
        assertThat(adminRoutes()).as("rutas de administración revisadas").isNotEmpty();
        assertThat(wrong).as("rutas de administración que el cajero puede abrir").isEmpty();
    }

    @Test
    void theCashierCanUseEverySalesRoute() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Route route : routes) {
            if (isCashierRoute(route.path())) {
                int status = perform(route, CASHIER, true).getStatus();
                if (status == 401 || status == 403) {
                    wrong.add(route + " -> " + status);
                }
            }
        }
        assertThat(wrong).as("rutas de ventas que el cajero no puede usar").isEmpty();
    }

    @Test
    void theAdministratorCanReachEveryRoute() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Route route : protectedRoutes()) {
            MockHttpServletResponse response = perform(route, ADMIN, true);
            if (response.getStatus() == 401 || response.getStatus() == 403
                    || "/login".equals(response.getRedirectedUrl())) {
                wrong.add(route + " -> " + response.getStatus());
            }
        }
        assertThat(wrong).as("rutas que el administrador no puede abrir").isEmpty();
    }

    /** Sin el token CSRF, ningún POST pasa, ni siquiera el del administrador. */
    @Test
    void everyPostWithoutTheCsrfTokenIsRejected() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (Route route : routes) {
            if (route.method() == HttpMethod.POST) {
                int status = perform(route, ADMIN, false).getStatus();
                if (status != 403) {
                    wrong.add(route + " -> " + status);
                }
            }
        }
        assertThat(wrong).as("POST aceptados sin token CSRF").isEmpty();
    }

    private MockHttpServletResponse perform(Route route, RequestPostProcessor user, boolean withCsrf)
            throws Exception {
        MockHttpServletRequestBuilder request = request(route.method(), route.samplePath());
        if (user != null) {
            request.with(user);
        }
        if (withCsrf) {
            request.with(csrf());
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    private List<Route> protectedRoutes() {
        return routes.stream().filter(route -> !PUBLIC_ROUTES.contains(route.path())).toList();
    }

    private Set<Route> adminRoutes() {
        Set<Route> admin = new TreeSet<>();
        protectedRoutes().stream().filter(route -> !isCashierRoute(route.path())).forEach(admin::add);
        return admin;
    }

    /** Lo único que ve el cajero: Ventas, y el inicio, que lo redirige a Ventas. */
    private static boolean isCashierRoute(String path) {
        return path.equals("/") || path.equals("/pos") || path.startsWith("/pos/");
    }

    private record Route(HttpMethod method, String path) implements Comparable<Route> {

        /** La ruta con un valor de ejemplo en cada variable: /admin/products/{id}/edit → /admin/products/1/edit. */
        String samplePath() {
            return path.replaceAll("\\{[^}]+}", "1");
        }

        @Override
        public int compareTo(Route other) {
            return toString().compareTo(other.toString());
        }

        @Override
        public String toString() {
            return method.name() + " " + path;
        }
    }
}
