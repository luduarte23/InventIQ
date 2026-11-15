package com.inventiq.web.layout;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.support.TestUsers;

/**
 * Layout base (D1): todas las pantallas traen el viewport y la hoja de estilos compartida,
 * y el menú muestra las pantallas de administración solo al ADMIN.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LayoutTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void theLoginPageUsesTheSharedHeadButHasNoMenu() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"viewport\"")))
                .andExpect(content().string(containsString("/css/app.css")))
                .andExpect(content().string(not(containsString("class=\"barra\""))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/pos", "/replenishment"})
    void everyAdminScreenHasTheViewportTheStylesAndTheMenu(String url) throws Exception {
        mockMvc.perform(get(url).with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"viewport\"")))
                .andExpect(content().string(containsString("/css/app.css")))
                .andExpect(content().string(containsString("class=\"barra\"")));
    }

    @Test
    void theAdminSeesTheAdminLinksAndTheirName() throws Exception {
        mockMvc.perform(get("/pos").with(TestUsers.admin()))
                .andExpect(content().string(containsString("href=\"/replenishment\"")))
                .andExpect(content().string(containsString(TestUsers.ADMIN.username())));
    }

    @Test
    void theCashierOnlySeesTheSalesLink() throws Exception {
        mockMvc.perform(get("/pos").with(TestUsers.cashier()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/pos\"")))
                .andExpect(content().string(not(containsString("href=\"/replenishment\""))))
                .andExpect(content().string(containsString(TestUsers.CASHIER.username())));
    }
}