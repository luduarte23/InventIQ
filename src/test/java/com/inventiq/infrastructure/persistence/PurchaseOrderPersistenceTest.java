/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.inventiq.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.model.PurchaseOrderLine;
import com.inventiq.infrastructure.persistence.adapter.PurchaseOrderRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba el adaptador de PurchaseOrder (A5) contra H2 con las tablas reales de Flyway:
 * la orden se guarda y se lee con sus líneas, y los cambios de estado quedan en la base.
 */
@DataJpaTest
@Import(PurchaseOrderRepositoryAdapter.class)
class PurchaseOrderPersistenceTest {

    private static final Instant CREATED = Instant.parse("2026-11-02T13:00:00Z");
    private static final Instant APPROVED = Instant.parse("2026-11-02T14:00:00Z");
    private static final Instant SENT = Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant RECEIVED = Instant.parse("2026-11-05T16:00:00Z");

    @Autowired private PurchaseOrderRepositoryAdapter orders;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private Long supplierId;
    private Long yogurId;
    private Long lecheId;

    /** La orden y sus líneas apuntan a un proveedor y a productos que deben existir (llaves foráneas). */
    @BeforeEach
    void setUp() {
        jdbc.update("insert into suppliers (name, whatsapp_number, lead_time_days) values (?, ?, ?)",
                "Lácteos del Valle", "+573001234567", 3);
        supplierId = jdbc.queryForObject("select id from suppliers", Long.class);
        yogurId = insertProduct("7702001000012", "Yogur de fresa");
        lecheId = insertProduct("7702001000029", "Leche entera");
    }

    @Test
    void savesAnOrderWithItsLinesAndReadsItBack() {
        PurchaseOrder draft = PurchaseOrder.draft(supplierId, CREATED);
        draft.addLine(line(yogurId, 10, "1000"));
        draft.addLine(line(lecheId, 2, "2500"));

        Long id = orders.save(draft).getId();
        reloadFromDatabase();
        PurchaseOrder read = orders.findById(id).get();

        assertNotNull(id);
        assertEquals(OrderStatus.DRAFT, read.getStatus());
        assertEquals(supplierId, read.getSupplierId());
        assertEquals(CREATED, read.getCreatedAt());
        assertEquals(2, read.getLines().size());
        assertEquals(id, read.getLines().get(0).getOrderId());
        assertEquals(new BigDecimal("15000.00"), read.getTotalCost());
        assertEquals(new BigDecimal("15000.00"),
                jdbc.queryForObject("select total_cost from purchase_orders", BigDecimal.class));
    }

    @Test
    void stateChangesAndReceivedUnitsArePersisted() {
        PurchaseOrder order = PurchaseOrder.draft(supplierId, CREATED);
        order.addLine(line(yogurId, 10, "1000"));
        order.approve(APPROVED);
        order.markSent(SENT);
        Long id = orders.save(order).getId();
        reloadFromDatabase();

        PurchaseOrder sent = orders.findById(id).get();
        sent.registerReception(yogurId, 4, RECEIVED);
        orders.save(sent);
        reloadFromDatabase();

        PurchaseOrder read = orders.findById(id).get();
        assertEquals(OrderStatus.PARTIALLY_RECEIVED, read.getStatus());
        assertEquals(APPROVED, read.getApprovedAt());
        assertEquals(SENT, read.getSentAt());
        assertNull(read.getReceivedAt());
        assertEquals(4, read.getLines().get(0).getQuantityReceived());
        assertEquals(6, read.getLines().get(0).pendingQuantity());
    }

    @Test
    void removingALineDeletesItsRow() {
        PurchaseOrder draft = PurchaseOrder.draft(supplierId, CREATED);
        draft.addLine(line(yogurId, 10, "1000"));
        draft.addLine(line(lecheId, 2, "2500"));
        Long id = orders.save(draft).getId();
        reloadFromDatabase();

        PurchaseOrder saved = orders.findById(id).get();
        saved.removeLine(lecheId);
        orders.save(saved);
        reloadFromDatabase();

        assertEquals(1, orders.findById(id).get().getLines().size());
        assertEquals(1, jdbc.queryForObject("select count(*) from purchase_order_lines", Integer.class));
    }

    @Test
    void findByStatusOnlyReturnsOrdersInThatStatus() {
        orders.save(draftWithOneLine());
        orders.save(draftWithOneLine());
        PurchaseOrder approved = draftWithOneLine();
        approved.approve(APPROVED);
        orders.save(approved);
        reloadFromDatabase();

        assertEquals(2, orders.findByStatus(OrderStatus.DRAFT).size());
        assertEquals(1, orders.findByStatus(OrderStatus.APPROVED).size());
        assertTrue(orders.findByStatus(OrderStatus.SENT).isEmpty());
    }

    @Test
    void anOrderWithoutSupplierIsSavedForReview() {
        PurchaseOrder draft = PurchaseOrder.draft(null, CREATED);
        draft.addLine(line(yogurId, 5, "1000"));

        Long id = orders.save(draft).getId();
        reloadFromDatabase();
        PurchaseOrder read = orders.findById(id).get();

        assertNull(read.getSupplierId());
        assertTrue(read.isNeedsReview());
    }

    @Test
    void anUnknownOrderIdReturnsEmpty() {
        assertTrue(orders.findById(999L).isEmpty());
    }

    // --- Ayudas ---

    private PurchaseOrder draftWithOneLine() {
        PurchaseOrder draft = PurchaseOrder.draft(supplierId, CREATED);
        draft.addLine(line(yogurId, 10, "1000"));
        return draft;
    }

    private static PurchaseOrderLine line(Long productId, int quantity, String unitCost) {
        return new PurchaseOrderLine(null, null, productId, quantity, 0, new BigDecimal(unitCost), false);
    }

    private Long insertProduct(String barcode, String name) {
        jdbc.update("insert into products (barcode, name, sale_price, unit_cost, perishable, safety_stock) "
                + "values (?, ?, ?, ?, ?, ?)", barcode, name, new BigDecimal("3200"), new BigDecimal("2100"), true, 0);
        return jdbc.queryForObject("select id from products where barcode = ?", Long.class, barcode);
    }

    /** Escribe en la base lo pendiente y olvida lo que JPA tenía en memoria, para leer de verdad de H2. */
    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }
}
