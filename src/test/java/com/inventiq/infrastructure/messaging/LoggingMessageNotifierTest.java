package com.inventiq.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.port.NotificationResult;
import org.junit.jupiter.api.Test;

/** Pruebas del notificador falso (A6): Java puro, sin Spring. */
class LoggingMessageNotifierTest {

    private static final String NUMBER = "+573001234567";

    private final LoggingMessageNotifier notifier = new LoggingMessageNotifier();

    @Test
    void aValidMessageIsSentWithAnId() {
        NotificationResult result = notifier.send(NUMBER, "Pedido #5 para Lácteos del Valle");

        assertTrue(result.success());
        assertTrue(result.providerMessageId().startsWith("log-"));
        assertNull(result.errorMessage());
    }

    @Test
    void eachMessageGetsItsOwnId() {
        String first = notifier.send(NUMBER, "Mensaje 1").providerMessageId();
        String second = notifier.send(NUMBER, "Mensaje 2").providerMessageId();

        assertNotEquals(first, second);
    }

    @Test
    void anInvalidNumberFailsWithoutThrowing() {
        // §5.6: nunca lanza excepción; devuelve success = false.
        for (String invalid : new String[] {null, "", "3001234567", "+57abc", "+57 300 123 4567"}) {
            NotificationResult result = notifier.send(invalid, "Hola");

            assertFalse(result.success());
            assertNull(result.providerMessageId());
            assertNotNull(result.errorMessage());
        }
    }

    @Test
    void anEmptyMessageFails() {
        assertFalse(notifier.send(NUMBER, null).success());
        assertFalse(notifier.send(NUMBER, "   ").success());
    }

    @Test
    void theLogOnlyShowsTheStartAndEndOfTheNumber() {
        assertEquals("+57******4567", LoggingMessageNotifier.mask(NUMBER));
    }
}
