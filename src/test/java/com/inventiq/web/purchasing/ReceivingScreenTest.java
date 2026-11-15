package com.inventiq.web.purchasing;

import java.math.BigDecimal;
import java.time.Instant;

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
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.support.TestUsers;

/**
 * Pantalla de recepción (FR-10, HU-05) de punta a punta, con un pedido enviado de 50 unidades
 * de un producto perecedero. Usa el "admin" real porque la recepción se audita.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReceivingScreenTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private PurchaseOrderRepository orders;

    private Long productId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        productId = products.save(new Product(null, "9990007778889", "Yogur de prueba D6",
                new BigDecimal("3200"), new BigDecimal("2100"), true, null, 0, 0, true, null)).getId();
        Instant now = Instant.parse("2026-11-15T15:00:00Z");
        PurchaseOrder order = PurchaseOrder.draft(null, now);
        order.addLine(new PurchaseOrderLine(null, null, productId, 50, 0, new BigDecimal("2100"), false));
        order.approve(now);
        order.markSent(now);
        orderId = orders.save(order).getId();
    }

    private RequestPostProcessor realAdmin() {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        return TestUsers.as(new AuthenticatedUser(id, "admin", Role.ADMIN));
    }

    @Test
    void theListAndTheOrderFormOpen() throws Exception {
        mockMvc.perform(get("/receiving").with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("#" + orderId)));
        mockMvc.perform(get("/receiving/" + orderId).with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Yogur de prueba D6")));
    }

    @Test
    void thirtyOfFiftyArriveThenTheOrderIsClosed() throws Exception {
        mockMvc.perform(post("/receiving/" + orderId).with(realAdmin()).with(csrf())
                        .param("productId", productId.toString())
                        .param("quantity", "30")
                        .param("expirationDate", "2099-12-20"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receiving/" + orderId));
        assertEquals(OrderStatus.PARTIALLY_RECEIVED, orders.findById(orderId).orElseThrow().getStatus());

        mockMvc.perform(post("/receiving/" + orderId + "/close").with(realAdmin()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/receiving"));
        assertEquals(OrderStatus.CLOSED, orders.findById(orderId).orElseThrow().getStatus());
    }

    @Test
    void aPerishableWithoutDateShowsTheError() throws Exception {
        mockMvc.perform(post("/receiving/" + orderId).with(realAdmin()).with(csrf())
                        .param("productId", productId.toString())
                        .param("quantity", "50")
                        .param("expirationDate", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error",
                        "Yogur de prueba D6 es perecedero: falta la fecha de vencimiento"));
        assertEquals(OrderStatus.SENT, orders.findById(orderId).orElseThrow().getStatus());
    }
}