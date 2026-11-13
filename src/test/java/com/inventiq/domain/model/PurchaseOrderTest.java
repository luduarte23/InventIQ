package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de PurchaseOrder: edición de líneas y transiciones de estado de ACUERDOS §4.
 * Java puro, sin Spring ni base de datos.
 */
class PurchaseOrderTest {

    private static final Instant CREATED = Instant.parse("2026-11-02T13:00:00Z");
    private static final Instant APPROVED = Instant.parse("2026-11-02T14:00:00Z");
    private static final Instant SENT = Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant RECEIVED = Instant.parse("2026-11-05T16:00:00Z");

    // --- Creación ---

    @Test
    void aDraftWithSupplierStartsEmptyAndWithoutReview() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        assertNull(order.getId());
        assertEquals(9L, order.getSupplierId());
        assertEquals(OrderStatus.DRAFT, order.getStatus());
        assertFalse(order.isNeedsReview());
        assertFalse(order.isDeferredByBudget());
        assertEquals(CREATED, order.getCreatedAt());
        assertNull(order.getApprovedAt());
        assertNull(order.getSentAt());
        assertNull(order.getReceivedAt());
        assertTrue(order.getLines().isEmpty());
        assertEquals(BigDecimal.ZERO, order.getTotalCost());
    }

    @Test
    void aDraftWithoutSupplierIsMarkedForReview() {
        PurchaseOrder order = PurchaseOrder.draft(null, CREATED);

        assertTrue(order.isNeedsReview());
    }

    @Test
    void statusAndCreatedAtAreRequired() {
        assertThrows(NullPointerException.class, () -> new PurchaseOrder(null, 9L, null,
                false, false, CREATED, null, null, null, List.of()));
        assertThrows(NullPointerException.class, () -> new PurchaseOrder(null, 9L,
                OrderStatus.DRAFT, false, false, null, null, null, null, List.of()));
    }

    @Test
    void nullLinesAreTreatedAsAnEmptyList() {
        PurchaseOrder order = new PurchaseOrder(1L, 9L, OrderStatus.DRAFT, false, false,
                CREATED, null, null, null, null);

        assertTrue(order.getLines().isEmpty());
    }

    @Test
    void theTotalIsCalculatedFromTheLinesItIsBuiltWith() {
        PurchaseOrder order = new PurchaseOrder(1L, 9L, OrderStatus.DRAFT, false, false,
                CREATED, null, null, null, List.of(line(1L, 10, "1000"), line(2L, 2, "2500")));

        assertEquals(new BigDecimal("15000.00"), order.getTotalCost());
    }

    // --- Edición de líneas (solo en DRAFT) ---

    @Test
    void addingLinesUpdatesTheTotal() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        order.addLine(line(1L, 10, "1000"));
        order.addLine(line(2L, 2, "2500"));

        assertEquals(2, order.getLines().size());
        assertEquals(new BigDecimal("15000.00"), order.getTotalCost());
    }

    @Test
    void theSameProductCannotBeAddedTwice() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);
        order.addLine(line(1L, 10, "1000"));

        assertThrows(IllegalStateException.class, () -> order.addLine(line(1L, 5, "1000")));
    }

    @Test
    void aNullLineIsRejected() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        assertThrows(NullPointerException.class, () -> order.addLine(null));
    }

    @Test
    void updateLineChangesQuantityMoqFlagAndTotal() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);
        order.addLine(line(1L, 10, "1000"));

        order.updateLine(1L, 12, true);

        PurchaseOrderLine updated = order.getLines().get(0);
        assertEquals(12, updated.getQuantityOrdered());
        assertTrue(updated.isAddedByMoq());
        assertEquals(new BigDecimal("12000.00"), order.getTotalCost());
    }

    @Test
    void updatingOrRemovingAProductThatIsNotInTheOrderFails() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);
        order.addLine(line(1L, 10, "1000"));

        assertThrows(IllegalArgumentException.class, () -> order.updateLine(99L, 5, false));
        assertThrows(IllegalArgumentException.class, () -> order.removeLine(99L));
    }

    @Test
    void removeLineTakesTheLineOutAndUpdatesTheTotal() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);
        order.addLine(line(1L, 10, "1000"));
        order.addLine(line(2L, 2, "2500"));

        order.removeLine(1L);

        assertEquals(1, order.getLines().size());
        assertEquals(new BigDecimal("5000.00"), order.getTotalCost());
    }

    @Test
    void linesCanOnlyBeEditedInDraft() {
        PurchaseOrder order = approvedOrder();

        assertThrows(IllegalStateException.class, () -> order.addLine(line(2L, 1, "1000")));
        assertThrows(IllegalStateException.class, () -> order.updateLine(1L, 5, false));
        assertThrows(IllegalStateException.class, () -> order.removeLine(1L));
    }

    @Test
    void theLineListCannotBeModifiedFromOutside() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        assertThrows(UnsupportedOperationException.class,
                () -> order.getLines().add(line(1L, 1, "1000")));
    }

    // --- Banderas ---

    @Test
    void reviewAndBudgetFlagsCanBeTurnedOnAndOff() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        order.markForReview();
        order.deferByBudget();
        assertTrue(order.isNeedsReview());
        assertTrue(order.isDeferredByBudget());

        order.clearReview();
        order.clearBudgetDeferral();
        assertFalse(order.isNeedsReview());
        assertFalse(order.isDeferredByBudget());
    }

    // --- Transiciones: DRAFT -> APPROVED -> SENT -> PARTIALLY_RECEIVED -> RECEIVED ---

    @Test
    void approveMovesTheDraftToApproved() {
        PurchaseOrder order = draftWithOneLine();

        order.approve(APPROVED);

        assertEquals(OrderStatus.APPROVED, order.getStatus());
        assertEquals(APPROVED, order.getApprovedAt());
    }

    @Test
    void anOrderWithoutLinesCannotBeApproved() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);

        assertThrows(IllegalStateException.class, () -> order.approve(APPROVED));
    }

    @Test
    void onlyADraftCanBeApproved() {
        PurchaseOrder order = approvedOrder();

        assertThrows(IllegalStateException.class, () -> order.approve(APPROVED));
    }

    @Test
    void markSentMovesTheApprovedOrderToSent() {
        PurchaseOrder order = approvedOrder();

        order.markSent(SENT);

        assertEquals(OrderStatus.SENT, order.getStatus());
        assertEquals(SENT, order.getSentAt());
    }

    @Test
    void aDraftCannotBeSentWithoutApproval() {
        PurchaseOrder order = draftWithOneLine();

        assertThrows(IllegalStateException.class, () -> order.markSent(SENT));
    }

    @Test
    void aPartialReceptionLeavesTheOrderPartiallyReceived() {
        PurchaseOrder order = sentOrder();

        order.registerReception(1L, 4, RECEIVED);

        assertEquals(OrderStatus.PARTIALLY_RECEIVED, order.getStatus());
        assertEquals(6, order.getLines().get(0).pendingQuantity());
        assertNull(order.getReceivedAt());
    }

    @Test
    void receivingEverythingLeavesTheOrderReceived() {
        PurchaseOrder order = sentOrder();

        order.registerReception(1L, 4, RECEIVED);
        order.registerReception(1L, 6, RECEIVED);

        assertEquals(OrderStatus.RECEIVED, order.getStatus());
        assertEquals(RECEIVED, order.getReceivedAt());
    }

    @Test
    void aSentOrderCanBeReceivedCompleteInOneStep() {
        PurchaseOrder order = sentOrder();

        order.registerReception(1L, 10, RECEIVED);

        assertEquals(OrderStatus.RECEIVED, order.getStatus());
    }

    @Test
    void onlySentOrPartiallyReceivedOrdersCanBeReceived() {
        PurchaseOrder draft = draftWithOneLine();
        PurchaseOrder received = sentOrder();
        received.registerReception(1L, 10, RECEIVED);

        assertThrows(IllegalStateException.class, () -> draft.registerReception(1L, 1, RECEIVED));
        assertThrows(IllegalStateException.class, () -> received.registerReception(1L, 1, RECEIVED));
    }

    @Test
    void receivingAProductThatIsNotInTheOrderFails() {
        PurchaseOrder order = sentOrder();

        assertThrows(IllegalArgumentException.class, () -> order.registerReception(99L, 1, RECEIVED));
    }

    // --- Cancelación: DRAFT | APPROVED | SENT -> CANCELLED ---

    @Test
    void draftApprovedAndSentOrdersCanBeCancelled() {
        PurchaseOrder draft = draftWithOneLine();
        PurchaseOrder approved = approvedOrder();
        PurchaseOrder sent = sentOrder();

        draft.cancel();
        approved.cancel();
        sent.cancel();

        assertEquals(OrderStatus.CANCELLED, draft.getStatus());
        assertEquals(OrderStatus.CANCELLED, approved.getStatus());
        assertEquals(OrderStatus.CANCELLED, sent.getStatus());
    }

    @Test
    void partiallyReceivedReceivedAndCancelledOrdersCannotBeCancelled() {
        PurchaseOrder partial = sentOrder();
        partial.registerReception(1L, 4, RECEIVED);
        PurchaseOrder received = sentOrder();
        received.registerReception(1L, 10, RECEIVED);
        PurchaseOrder cancelled = draftWithOneLine();
        cancelled.cancel();

        assertThrows(IllegalStateException.class, partial::cancel);
        assertThrows(IllegalStateException.class, received::cancel);
        assertThrows(IllegalStateException.class, cancelled::cancel);
    }

    // --- Igualdad por id ---

    @Test
    void ordersWithTheSameIdAreEqual() {
        PurchaseOrder a = new PurchaseOrder(5L, 9L, OrderStatus.DRAFT, false, false,
                CREATED, null, null, null, List.of());
        PurchaseOrder b = new PurchaseOrder(5L, 8L, OrderStatus.SENT, false, false,
                CREATED, null, null, null, List.of());

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void ordersWithoutIdAreOnlyEqualToThemselves() {
        PurchaseOrder a = PurchaseOrder.draft(9L, CREATED);
        PurchaseOrder b = PurchaseOrder.draft(9L, CREATED);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }

    // --- Ayudas para armar órdenes en cada estado ---

    private static PurchaseOrderLine line(Long productId, int quantity, String unitCost) {
        return new PurchaseOrderLine(null, null, productId, quantity, 0,
                new BigDecimal(unitCost), false);
    }

    /** Borrador con 10 unidades del producto 1. */
    private static PurchaseOrder draftWithOneLine() {
        PurchaseOrder order = PurchaseOrder.draft(9L, CREATED);
        order.addLine(line(1L, 10, "1000"));
        return order;
    }

    private static PurchaseOrder approvedOrder() {
        PurchaseOrder order = draftWithOneLine();
        order.approve(APPROVED);
        return order;
    }

    private static PurchaseOrder sentOrder() {
        PurchaseOrder order = approvedOrder();
        order.markSent(SENT);
        return order;
    }
}