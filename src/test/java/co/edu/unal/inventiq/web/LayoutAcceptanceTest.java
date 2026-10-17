package co.edu.unal.inventiq.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import jakarta.servlet.RequestDispatcher;

/** Estructura común: barra lateral con el ítem activo, inicio y página de error. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:layout;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class LayoutAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void theSidebarLinksEveryModuleAndKeepsTheLogoInTheTopBar() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-sidebar-toggle")))
                .andExpect(content().string(containsString("class=\"brand-name\">InventIQ")))
                .andExpect(content().string(containsString("href=\"/pos\"")))
                .andExpect(content().string(containsString("href=\"/inventory\"")))
                .andExpect(content().string(containsString("href=\"/admin/products\"")))
                .andExpect(content().string(containsString("href=\"/admin/history\"")))
                .andExpect(content().string(containsString("Reabastecimiento")));
    }

    @Test
    void theCurrentPageIsMarkedAsActiveInTheSidebar() throws Exception {
        assertThat(activeLinkOf("/")).contains("Inicio");
        assertThat(activeLinkOf("/pos")).contains("Ventas");
        assertThat(activeLinkOf("/inventory")).contains("Inventario");
        assertThat(activeLinkOf("/admin/products")).contains("Productos");
        assertThat(activeLinkOf("/admin/history")).contains("Historial");
    }

    @Test
    void theHomePageSummarizesTheDay() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Ventas de hoy")))
                .andExpect(content().string(containsString("Lotes por vencer")))
                .andExpect(content().string(containsString("Necesitan atención")))
                .andExpect(content().string(containsString("Accesos rápidos")));
    }

    @Test
    void errorsUseTheSameDesign() throws Exception {
        // Un navegador pide HTML; sin eso Spring responde el error en JSON.
        mvc.perform(get("/error").accept(MediaType.TEXT_HTML)
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/no-existe"))
                .andExpect(content().string(containsString("No encontramos esa página")))
                .andExpect(content().string(containsString("data-sidebar-toggle")));
    }

    /** El enlace marcado con aria-current="page" en la barra lateral. */
    private String activeLinkOf(String path) throws Exception {
        String html = mvc.perform(get(path)).andReturn().getResponse().getContentAsString();
        int sidebar = html.indexOf("id=\"sidebar\"");
        int marker = html.indexOf("aria-current=\"page\"", sidebar);
        assertThat(marker).as("enlace activo en " + path).isPositive();
        int start = html.lastIndexOf("<a ", marker);
        int end = html.indexOf("</a>", marker);
        return html.substring(start, end);
    }
}
