package com.inventiq.web.admin;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.support.TestUsers;

/** Pantalla de ajuste de stock (FR-08) de punta a punta, con un producto y un lote de prueba. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StockScreenTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private BatchRepository batches;

    private Long productId;
    private Long batchId;

    @BeforeEach
    void setUp() {
        productId = products.save(new Product(null, "9990005556667", "Producto de prueba D5",
                new BigDecimal("5000"), new BigDecimal("3000"), false, null, 0, 0, true, null)).getId();
        batchId = batches.save(new Batch(null, productId, 10, 6, LocalDate.of(2099, 1, 1),
                Instant.parse("2026-11-01T15:00:00Z"), new BigDecimal("3000"), null, null, null)).getId();
    }

    private RequestPostProcessor realAdmin() {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        return TestUsers.as(new AuthenticatedUser(id, "admin", Role.ADMIN));
    }

    @Test
    void theListAndTheProductBatchesOpen() throws Exception {
        mockMvc.perform(get("/stock").with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Producto de prueba D5")));
        mockMvc.perform(get("/stock/" + productId).with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("01/01/2099")));
    }

    @Test
    void aBatchIsAdjustedWithAReason() throws Exception {
        mockMvc.perform(post("/stock/" + productId + "/batches/" + batchId).with(realAdmin()).with(csrf())
                        .param("newQuantity", "4")
                        .param("reason", "Se dañaron 2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/stock/" + productId));

        assertEquals(4, batches.findById(batchId).orElseThrow().getQuantityRemaining());
    }

    @Test
    void anAdjustmentWithoutReasonShowsTheError() throws Exception {
        mockMvc.perform(post("/stock/" + productId + "/batches/" + batchId).with(realAdmin()).with(csrf())
                        .param("newQuantity", "4")
                        .param("reason", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "El motivo del ajuste es obligatorio"));
    }
}