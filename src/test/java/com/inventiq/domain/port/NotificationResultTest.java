package com.inventiq.domain.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de NotificationResult (ACUERDOS §5.6 y §6.7). */
class NotificationResultTest {

    @Test
    void okIsASuccessWithTheProviderId() {
        NotificationResult result = NotificationResult.ok("SM123");

        assertTrue(result.success());
        assertEquals("SM123", result.providerMessageId());
        assertNull(result.errorMessage());
    }

    @Test
    void failedIsNotASuccessAndKeepsTheError() {
        NotificationResult result = NotificationResult.failed("Número no válido");

        assertFalse(result.success());
        assertNull(result.providerMessageId());
        assertEquals("Número no válido", result.errorMessage());
    }
}
