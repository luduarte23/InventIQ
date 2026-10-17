package co.edu.unal.inventiq.domain.model;

import static co.edu.unal.inventiq.domain.model.OrderStatus.APPROVED;
import static co.edu.unal.inventiq.domain.model.OrderStatus.DRAFT;
import static co.edu.unal.inventiq.domain.model.OrderStatus.PARTIALLY_RECEIVED;
import static co.edu.unal.inventiq.domain.model.OrderStatus.RECEIVED;
import static co.edu.unal.inventiq.domain.model.OrderStatus.SENT;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    @Test
    void followsTheAgreedSequence() {
        assertThat(DRAFT.nextStatuses()).containsExactly(APPROVED);
        assertThat(APPROVED.nextStatuses()).containsExactly(SENT);
        assertThat(SENT.nextStatuses()).containsExactlyInAnyOrder(PARTIALLY_RECEIVED, RECEIVED);
        assertThat(PARTIALLY_RECEIVED.nextStatuses()).containsExactlyInAnyOrder(PARTIALLY_RECEIVED, RECEIVED);
        assertThat(RECEIVED.nextStatuses()).isEmpty();
    }

    @Test
    void cannotSkipOrGoBack() {
        assertThat(DRAFT.canTransitionTo(SENT)).isFalse();
        assertThat(DRAFT.canTransitionTo(RECEIVED)).isFalse();
        assertThat(APPROVED.canTransitionTo(DRAFT)).isFalse();
        assertThat(RECEIVED.canTransitionTo(DRAFT)).isFalse();
        assertThat(DRAFT.canTransitionTo(APPROVED)).isTrue();
    }

    @Test
    void onlyDraftsAreEditableAndOnlySentOrdersAwaitReceipt() {
        assertThat(DRAFT.isEditable()).isTrue();
        assertThat(APPROVED.isEditable()).isFalse();
        assertThat(SENT.isAwaitingReceipt()).isTrue();
        assertThat(PARTIALLY_RECEIVED.isAwaitingReceipt()).isTrue();
        assertThat(APPROVED.isAwaitingReceipt()).isFalse();
        assertThat(RECEIVED.isAwaitingReceipt()).isFalse();
    }

    @Test
    void hasSpanishLabels() {
        assertThat(PARTIALLY_RECEIVED.label()).isEqualTo("Recibido en parte");
    }
}
