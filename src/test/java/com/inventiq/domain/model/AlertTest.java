package com.inventiq.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pruebas de Alert: creación, validaciones, resolución e isAbout. */
class AlertTest {

    private static final Instant NOW = Instant.parse("2026-11-05T11:00:00Z");

    @Test
    void raiseCreatesAnUnresolvedAlertWithoutId() {
        Alert alert = Alert.raise(AlertType.LOW_STOCK, "Quedan 2 yogures", "Product", 1L, NOW);

        assertNull(alert.getId());
        assertEquals(NOW, alert.getCreatedAt());
        assertEquals(AlertType.LOW_STOCK, alert.getType());
        assertEquals("Quedan 2 yogures", alert.getMessage());
        assertEquals("Product", alert.getRelatedEntityType());
        assertEquals(1L, alert.getRelatedEntityId());
        assertFalse(alert.isResolved());
    }

    @Test
    void aGeneralAlertHasNoRelatedEntity() {
        Alert alert = Alert.raise(AlertType.NOTIFICATION_FAILED, "Twilio no respondió", null, null, NOW);

        assertNull(alert.getRelatedEntityType());
        assertNull(alert.getRelatedEntityId());
    }

    @Test
    void createdAtAndTypeAreRequired() {
        assertThrows(NullPointerException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, "msg", null, null, null));
        assertThrows(NullPointerException.class,
                () -> Alert.raise(null, "msg", null, null, NOW));
    }

    @Test
    void theMessageCannotBeEmptyNorLongerThan500Characters() {
        assertThrows(IllegalArgumentException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, null, null, null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, " ", null, null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, "x".repeat(501), null, null, NOW));
    }

    @Test
    void relatedTypeAndIdGoTogether() {
        assertThrows(IllegalArgumentException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, "msg", "Product", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Alert.raise(AlertType.LOW_STOCK, "msg", null, 1L, NOW));
    }

    @Test
    void resolveMarksTheAlertAsResolved() {
        Alert alert = Alert.raise(AlertType.LOW_STOCK, "msg", "Product", 1L, NOW);

        alert.resolve();

        assertTrue(alert.isResolved());
    }

    @Test
    void isAboutComparesTheRelatedEntity() {
        Alert alert = Alert.raise(AlertType.LOW_STOCK, "msg", "Product", 1L, NOW);

        assertTrue(alert.isAbout("Product", 1L));
        assertFalse(alert.isAbout("Product", 2L));
        assertFalse(alert.isAbout("Batch", 1L));
    }

    @Test
    void alertsWithTheSameIdAreEqual() {
        Alert a = new Alert(4L, NOW, AlertType.LOW_STOCK, "a", null, null, false);
        Alert b = new Alert(4L, NOW, AlertType.NOTIFICATION_FAILED, "b", null, null, true);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void alertsWithoutIdAreOnlyEqualToThemselves() {
        Alert a = Alert.raise(AlertType.LOW_STOCK, "msg", null, null, NOW);
        Alert b = Alert.raise(AlertType.LOW_STOCK, "msg", null, null, NOW);

        assertEquals(a, a);
        assertNotEquals(a, b);
        assertNotEquals(a, "otro tipo");
    }
}