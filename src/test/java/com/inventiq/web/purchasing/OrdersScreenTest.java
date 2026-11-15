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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.inventiq.application.security.AuthenticatedUser;
import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.domain.model.Role;
import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.domain.port.SupplierRepository;
import com.inventiq.domain.port.UserRepository;
import com.inventiq.support.TestUsers;

/**
 * Pantalla de pedidos (D7) de punta a punta: borrador -> editar -> aprobar desde la lista ->
 * enviar. Usa el "admin" real porque aprobar se audita.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OrdersScreenTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository users;
    @Autowired private SupplierRepository suppliers;
    @Autowired private ProductRepository products;
    @Autowired private PurchaseOrderRepository orders;

    private Long productId;
    private Long orderId;

    @BeforeEach
    void setUp() {
        Long supplierId = suppliers.save(new Supplier(null, "Proveedor de prueba D7", "+573001112233",
                2, null, null, true)).getId();
        productId = products.save(new Product(null, "9990009990001", "Producto de prueba D7",
                new BigDecimal("3000"), new BigDecimal("2000"), false, supplierId, 0, 0, true, null)).getId();
        PurchaseOrder draft = PurchaseOrder.draft(supplierId, Instant.parse("2026-11-15T15:00:00Z"));
        draft.addLine(new PurchaseOrderLine(null, null, productId, 10, 0, new BigDecimal("2000"), false));
        orderId = orders.save(draft).getId();
    }

    private RequestPostProcessor realAdmin() {
        Long id = users.findByUsername("admin").orElseThrow().getId();
        return TestUsers.as(new AuthenticatedUser(id, "admin", Role.ADMIN));
    }

    @Test
    void theListAndTheDetailOpen() throws Exception {
        mockMvc.perform(get("/orders").with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Proveedor de prueba D7")));
        mockMvc.perform(get("/orders/" + orderId).with(TestUsers.admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Producto de prueba D7")));
    }

    @Test
    void aDraftIsEditedApprovedFromTheListAndSent() throws Exception {
        mockMvc.perform(post("/orders/" + orderId + "/lines").with(realAdmin()).with(csrf())
                        .param("productId", productId.toString())
                        .param("quantity", "12")
                        .param("addedByMoq", "false"))
                .andExpect(redirectedUrl("/orders/" + orderId));
        assertEquals(12, orders.findById(orderId).orElseThrow().getLines().get(0).getQuantityOrdered());

        mockMvc.perform(post("/orders/" + orderId + "/approve").with(realAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/orders"));
        assertEquals(OrderStatus.APPROVED, orders.findById(orderId).orElseThrow().getStatus());

        mockMvc.perform(get("/orders/" + orderId).with(TestUsers.admin()))
                .andExpect(content().string(containsString("Hola Proveedor de prueba D7")));

        mockMvc.perform(post("/orders/" + orderId + "/send").with(realAdmin()).with(csrf()))
                .andExpect(redirectedUrl("/orders/" + orderId));
        assertEquals(OrderStatus.SENT, orders.findById(orderId).orElseThrow().getStatus());
    }
}