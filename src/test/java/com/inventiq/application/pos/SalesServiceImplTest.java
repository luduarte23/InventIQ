package com.inventiq.application.pos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.exception.UnknownBarcodeException;
import com.inventiq.fakes.InMemoryBatchRepository;
import com.inventiq.fakes.InMemoryProductRepository;
import com.inventiq.fakes.InMemorySaleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Pruebas del servicio de ventas (B4) con repositorios falsos: sin Spring ni base de datos. */
class SalesServiceImplTest {

    private InMemorySaleRepository sales;
    private SalesServiceImpl service;
    private Batch oct8;
    private Batch oct30;

    @BeforeEach
    void setUp() {
        InMemoryProductRepository products = new InMemoryProductRepository();
        InMemoryBatchRepository batches = new InMemoryBatchRepository();
        sales = new InMemorySaleRepository();
        ZoneId bogota = ZoneId.of("America/Bogota");
        Clock clock = Clock.fixed(LocalDate.of(2026, 10, 5).atTime(10, 0).atZone(bogota).toInstant(), bogota);
        service = new SalesServiceImpl(products, batches, sales, clock);

        // US-03: yogur con 6 unidades, 3 vencen el 08/10/2026 y 3 el 30/10/2026.
        products.save(new Product(1L, "7702001000012", "Yogur de fresa", new BigDecimal("3200"),
                new BigDecimal("2100"), true, null, 0, 0, true, 0L));
        oct30 = batches.save(batch(1L, LocalDate.of(2026, 10, 30)));
        oct8 = batches.save(batch(2L, LocalDate.of(2026, 10, 8)));
    }

    @Test
    void us03Scenario1_scanningTwiceDeductsTwoUnitsFromTheBatchThatExpiresFirst() {
        Sale sale = service.startSale(1L);

        service.scan(sale.getId(), "7702001000012", 1);
        ScanResult result = service.scan(sale.getId(), "7702001000012", 1);

        assertEquals(1, oct8.getQuantityRemaining());
        assertEquals(3, oct30.getQuantityRemaining());
        assertEquals(new BigDecimal("6400.00"), result.saleRunningTotal());
    }

    @Test
    void us03Scenario2_anUnknownBarcodeDoesNotChangeTheStockNorTheSale() {
        Sale sale = service.startSale(1L);

        assertThrows(UnknownBarcodeException.class, () -> service.scan(sale.getId(), "0000000000000", 1));

        assertEquals(3, oct8.getQuantityRemaining());
        assertEquals(3, oct30.getQuantityRemaining());
        assertTrue(sales.findById(sale.getId()).get().getItems().isEmpty());
    }

    @Test
    void finishingASaleClosesIt() {
        Sale sale = service.startSale(1L);
        service.scan(sale.getId(), "7702001000012", 2);

        assertTrue(service.finishSale(sale.getId()).isClosed());
    }

    @Test
    void cancellingASaleGivesTheUnitsBackToTheirBatchesAndDeletesIt() {
        Sale sale = service.startSale(1L);
        service.scan(sale.getId(), "7702001000012", 5);   // 3 del lote del 08/10 y 2 del lote del 30/10

        service.cancelSale(sale.getId());

        assertEquals(3, oct8.getQuantityRemaining());
        assertEquals(3, oct30.getQuantityRemaining());
        assertTrue(sales.findById(sale.getId()).isEmpty());
    }

    private static Batch batch(Long id, LocalDate expiration) {
        return new Batch(id, 1L, 3, 3, expiration, Instant.parse("2026-10-01T12:00:00Z"),
                new BigDecimal("2100"), BigDecimal.ZERO, null, 0L);
    }
}
