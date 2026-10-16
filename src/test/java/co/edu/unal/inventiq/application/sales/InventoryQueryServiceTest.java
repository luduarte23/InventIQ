package co.edu.unal.inventiq.application.sales;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.fakes.InMemoryBatchRepository;
import co.edu.unal.inventiq.fakes.InMemoryProductRepository;

class InventoryQueryServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 9, 28, 9, 0);

    private InMemoryProductRepository products;
    private InMemoryBatchRepository batches;
    private InventoryQueryService service;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductRepository();
        batches = new InMemoryBatchRepository();
        Clock clock = Clock.fixed(TODAY.atStartOfDay(BOGOTA).toInstant(), BOGOTA);
        service = new InventoryQueryService(products, batches, clock);
    }

    @Test
    void showsTheBatchesOfEachProductInTheOrderTheyWillBeSoldWithTheirStatus() {
        Product milk = products.add("7702001000028", "Leche", 4500);
        Batch later = batches.add(milk.id(), 5, TODAY.plusDays(10), RECEIVED);
        Batch expired = batches.add(milk.id(), 12, 2, TODAY.minusDays(1), RECEIVED);
        Batch sooner = batches.add(milk.id(), 4, TODAY.plusDays(2), RECEIVED);

        ProductStock row = service.overview().rows().get(0);

        assertThat(row.batches()).extracting(view -> view.batch().getId())
                .containsExactly(expired.getId(), sooner.getId(), later.getId());
        assertThat(row.batches()).extracting(BatchView::status)
                .containsExactly(ExpirationStatus.EXPIRED, ExpirationStatus.EXPIRING_SOON, ExpirationStatus.OK);
        assertThat(row.batches()).extracting(BatchView::daysUntilExpiration).containsExactly(-1L, 2L, 10L);
        assertThat(row.sellableUnits()).isEqualTo(9);
        assertThat(row.expiredUnits()).isEqualTo(2);
        assertThat(row.expiringSoonUnits()).isEqualTo(4);
    }

    @Test
    void theNextExpiringBatchIsTheMostUrgentOneAndIgnoresBatchesWithoutDate() {
        Product rice = products.add("1", "Arroz", 4800);
        batches.add(rice.id(), 10, null, RECEIVED);
        Product milk = products.add("2", "Leche", 4500);
        batches.add(milk.id(), 3, TODAY.plusDays(20), RECEIVED);
        Batch expired = batches.add(milk.id(), 5, 1, TODAY.minusDays(3), RECEIVED);

        List<ProductStock> rows = service.overview().rows();

        assertThat(rows.get(0).nextExpiring()).isNull();
        assertThat(rows.get(1).nextExpiring().batch().getId()).isEqualTo(expired.getId());
        assertThat(rows.get(1).nextExpiring().status()).isEqualTo(ExpirationStatus.EXPIRED);
    }

    @Test
    void listsActiveProductsByNameIncludingThoseWithoutStock() {
        products.add("2", "Kumis", 5200);
        products.add("1", "Arroz", 4800);
        products.add("3", "Retirado", 1000, false, null);

        List<ProductStock> rows = service.overview().rows();

        assertThat(rows).extracting(row -> row.product().name()).containsExactly("Arroz", "Kumis");
        assertThat(rows).allSatisfy(row -> {
            assertThat(row.hasStock()).isFalse();
            assertThat(row.sellableUnits()).isZero();
            assertThat(row.nextExpiring()).isNull();
        });
    }

    @Test
    void flagsProductsAtOrBelowTheirReorderPoint() {
        Product low = products.add("1", "Arroz", 4800, true, 10);
        Product ok = products.add("2", "Panela", 3000, true, 2);
        Product notCalculated = products.add("3", "Café", 9800, true, null);
        batches.add(low.id(), 10, null, RECEIVED);
        batches.add(ok.id(), 8, null, RECEIVED);
        batches.add(notCalculated.id(), 1, null, RECEIVED);

        List<ProductStock> rows = service.overview().rows();

        assertThat(rows).filteredOn(ProductStock::belowReorderPoint)
                .extracting(row -> row.product().name())
                .containsExactly("Arroz");
    }

    @Test
    void theSummaryCountsUnitsExpiringAndExpiredBatchesAndPausedProducts() {
        Product milk = products.add("1", "Leche", 4500);
        batches.add(milk.id(), 5, TODAY.plusDays(3), RECEIVED);
        batches.add(milk.id(), 6, TODAY.plusDays(30), RECEIVED);
        batches.add(milk.id(), 4, 2, TODAY.minusDays(1), RECEIVED);
        Product coffee = products.create(aProduct().id(null).barcode("2").name("Café").saleEnabled(false).build());
        batches.add(coffee.id(), 60, null, RECEIVED);
        products.add("3", "Arroz", 4800, true, 100);

        InventorySummary summary = service.overview().summary();

        assertThat(summary.products()).isEqualTo(3);
        assertThat(summary.sellableUnits()).isEqualTo(71);
        assertThat(summary.batchesExpiringSoon()).isEqualTo(1);
        assertThat(summary.batchesExpired()).isEqualTo(1);
        assertThat(summary.pausedProducts()).isEqualTo(1);
        assertThat(summary.belowReorderPoint()).isEqualTo(1);
    }

    @Test
    void rowsCanBeLookedUpByProductId() {
        Product milk = products.add("1", "Leche", 4500);

        assertThat(service.overview().byProductId()).containsOnlyKeys(milk.id());
    }
}
