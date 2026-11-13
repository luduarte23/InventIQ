package com.inventiq.domain.model.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de las excepciones de negocio: mensaje, datos y jerarquía. */
class DomainExceptionsTest {

    @Test
    void unknownBarcodeKeepsTheBarcode() {
        UnknownBarcodeException e = new UnknownBarcodeException("7700000000000");

        assertEquals("7700000000000", e.getBarcode());
        assertTrue(e.getMessage().contains("7700000000000"));
    }

    @Test
    void insufficientStockKeepsRequestedAndAvailableUnits() {
        InsufficientStockException e = new InsufficientStockException(1L, 5, 3);

        assertEquals(1L, e.getProductId());
        assertEquals(5, e.getRequested());
        assertEquals(3, e.getAvailable());
        assertTrue(e.getMessage().contains("5"));
    }

    @Test
    void invalidCredentialsAlwaysSaysTheSame() {
        // No dice si falló el usuario o la contraseña, para no revelar qué usuarios existen.
        assertEquals("Usuario o contraseña incorrectos",
                new InvalidCredentialsException().getMessage());
    }

    @Test
    void invalidSaleKeepsItsMessage() {
        assertEquals("La venta ya está cerrada",
                new InvalidSaleException("La venta ya está cerrada").getMessage());
    }

    @Test
    void allOfThemAreDomainExceptionsAndRuntimeExceptions() {
        RuntimeException[] all = {
            new UnknownBarcodeException("1"),
            new InsufficientStockException(1L, 1, 0),
            new InvalidCredentialsException(),
            new InvalidSaleException("x")
        };

        for (RuntimeException e : all) {
            assertTrue(e instanceof DomainException);
        }
    }
}