package co.edu.unal.inventiq.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.InvalidOrderTransitionException;
import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;
import co.edu.unal.inventiq.domain.exception.InvalidReceiptException;
import co.edu.unal.inventiq.domain.exception.OrderNotEditableException;
import co.edu.unal.inventiq.domain.exception.OrderWithoutSupplierException;

class PurchaseOrderTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 20, 10, 0);
    private static final long PRODUCT_A = 1;
    private static final long PRODUCT_P = 2;

    private static PurchaseOrder draftOf(Long supplierId) {
        return PurchaseOrder.draft(supplierId, List.of(
                OrderLine.of(PRODUCT_A, 50, new BigDecimal("2000")),
                OrderLine.of(PRODUCT_P, 50, new BigDecimal("1000"))), NOW);
    }

    private static PurchaseOrder sentOrder() {
        PurchaseOrder order = draftOf(7L);
        order.approve(NOW);
        order.markSent(NOW);
        return order;
    }

    @Test
    void newDraftHasTotalsAndNoReceivedUnits() {
        PurchaseOrder order = draftOf(7L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
        assertThat(order.getId()).isNull();
        assertThat(order.getSupplierId()).isEqualTo(7L);
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getVersion()).isZero();
        assertThat(order.needsReview()).isFalse();
        assertThat(order.total()).isEqualByComparingTo("150000");
        assertThat(order.totalUnits()).isEqualTo(100);
        assertThat(order.pendingLines()).hasSize(2);
    }

    @Test
    void orderWithoutSupplierNeedsReviewAndCannotBeApproved() {
        PurchaseOrder order = draftOf(null);

        assertThat(order.needsReview()).isTrue();
        assertThatThrownBy(() -> order.approve(NOW)).isInstanceOf(OrderWithoutSupplierException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    void draftQuantitiesAreEditable() {
        PurchaseOrder order = draftOf(7L);

        order.changeQuantity(PRODUCT_A, 60);

        assertThat(order.lineFor(PRODUCT_A).orElseThrow().getQuantityOrdered()).isEqualTo(60);
        assertThat(order.total()).isEqualByComparingTo("170000");
        assertThatThrownBy(() -> order.changeQuantity(PRODUCT_A, 0)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> order.changeQuantity(99, 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void approvedOrderIsNoLongerEditable() {
        PurchaseOrder order = draftOf(7L);
        order.approve(NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(order.getApprovedAt()).isEqualTo(NOW);
        assertThatThrownBy(() -> order.changeQuantity(PRODUCT_A, 10))
                .isInstanceOf(OrderNotEditableException.class)
                .hasMessageContaining("Aprobado");
    }

    @Test
    void cannotSendADraftOrApproveTwice() {
        PurchaseOrder draft = draftOf(7L);
        assertThatThrownBy(() -> draft.markSent(NOW)).isInstanceOf(InvalidOrderTransitionException.class);

        draft.approve(NOW);
        assertThatThrownBy(() -> draft.approve(NOW))
                .isInstanceOf(InvalidOrderTransitionException.class)
                .hasMessage("Un pedido en estado \"Aprobado\" no puede pasar a \"Aprobado\".");
    }

    @Test
    void cannotReceiveAnOrderThatWasNotSent() {
        PurchaseOrder order = draftOf(7L);
        order.approve(NOW);

        assertThatThrownBy(() -> order.receive(Map.of(PRODUCT_A, 50), NOW))
                .isInstanceOf(InvalidOrderTransitionException.class);
    }

    /** US-05 esc. 1: llega todo lo pedido. */
    @Test
    void fullReceiptMarksTheOrderAsReceived() {
        PurchaseOrder order = sentOrder();

        order.receive(Map.of(PRODUCT_A, 50, PRODUCT_P, 50), NOW.plusDays(3));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(order.getSentAt()).isEqualTo(NOW);
        assertThat(order.getReceivedAt()).isEqualTo(NOW.plusDays(3));
        assertThat(order.pendingLines()).isEmpty();
    }

    /** US-05 esc. 2: de 50 unidades llegan 30 y quedan 20 pendientes visibles. */
    @Test
    void partialReceiptKeepsPendingUnitsVisible() {
        PurchaseOrder order = sentOrder();

        order.receive(Map.of(PRODUCT_A, 50, PRODUCT_P, 30), NOW);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PARTIALLY_RECEIVED);
        assertThat(order.getReceivedAt()).isNull();
        assertThat(order.pendingLines()).singleElement().satisfies(line -> {
            assertThat(line.getProductId()).isEqualTo(PRODUCT_P);
            assertThat(line.getQuantityReceived()).isEqualTo(30);
            assertThat(line.pending()).isEqualTo(20);
        });

        order.receive(Map.of(PRODUCT_P, 20), NOW.plusDays(1));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.RECEIVED);
    }

    @Test
    void rejectsReceiptsThatDoNotMatchTheOrder() {
        PurchaseOrder order = sentOrder();

        assertThatThrownBy(() -> order.receive(Map.of(PRODUCT_A, 51), NOW))
                .isInstanceOf(InvalidReceiptException.class).hasMessageContaining("solo faltaban 50");
        assertThatThrownBy(() -> order.receive(Map.of(PRODUCT_A, -1), NOW))
                .isInstanceOf(InvalidReceiptException.class);
        assertThatThrownBy(() -> order.receive(Map.of(99L, 1), NOW))
                .isInstanceOf(InvalidReceiptException.class);
        assertThatThrownBy(() -> order.receive(Map.of(PRODUCT_A, 0), NOW))
                .isInstanceOf(InvalidReceiptException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SENT);
        assertThat(order.lineFor(PRODUCT_A).orElseThrow().getQuantityReceived()).isZero();
    }

    @Test
    void anOrderNeedsDistinctProductsAndAtLeastOneLine() {
        assertThatThrownBy(() -> PurchaseOrder.draft(7L, List.of(), NOW))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PurchaseOrder.draft(7L, List.of(
                OrderLine.of(PRODUCT_A, 1, BigDecimal.ONE),
                OrderLine.of(PRODUCT_A, 2, BigDecimal.ONE)), NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void orderLineValidatesItsQuantitiesAndCost() {
        assertThatThrownBy(() -> OrderLine.of(PRODUCT_A, 0, BigDecimal.ONE))
                .isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> new OrderLine(1L, PRODUCT_A, 5, 6, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderLine(1L, PRODUCT_A, 5, -1, BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrderLine.of(PRODUCT_A, 5, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);

        OrderLine line = new OrderLine(3L, PRODUCT_A, 5, 2, new BigDecimal("1500"));
        assertThat(line.getId()).isEqualTo(3L);
        assertThat(line.getUnitCost()).isEqualByComparingTo("1500");
        assertThat(line.subtotal()).isEqualByComparingTo("7500");
        assertThat(line.pending()).isEqualTo(3);
        assertThat(line.isComplete()).isFalse();
    }
}
