package co.edu.unal.inventiq.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SaleItem;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.BatchRepositoryAdapter;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.ProductRepositoryAdapter;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.SaleRepositoryAdapter;

/** Adaptadores de la Parte B contra H2 con el esquema real de Flyway (A5 y B3). */
@DataJpaTest
@Import({ProductRepositoryAdapter.class, BatchRepositoryAdapter.class, SaleRepositoryAdapter.class})
class SalesPersistenceTest {

    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 10, 1, 9, 0);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductRepositoryAdapter products;

    @Autowired
    private BatchRepositoryAdapter batches;

    @Autowired
    private SaleRepositoryAdapter sales;

    private long yogurtId;

    @BeforeEach
    void setUp() {
        yogurtId = insertProduct("7702001000011", "Yogur", true);
    }

    @Test
    void findsProductsByBarcodeAndListsOnlyActiveOnes() {
        insertProduct("7700000000001", "Retirado", false);

        Product yogurt = products.findByBarcode("7702001000011").orElseThrow();

        assertThat(yogurt.name()).isEqualTo("Yogur");
        assertThat(yogurt.salePrice()).isEqualByComparingTo("3200");
        assertThat(products.findById(yogurtId)).contains(yogurt);
        assertThat(products.findByBarcode("no-existe")).isEmpty();
        assertThat(products.findAllActive()).extracting(Product::name).containsExactly("Yogur");
    }

    @Test
    void availableBatchesComeInFifoOrderAndExcludeEmptyOnes() {
        long noExpiration = insertBatch(yogurtId, 5, 5, null, RECEIVED);
        long later = insertBatch(yogurtId, 3, 3, LocalDate.of(2026, 10, 30), RECEIVED);
        long sooner = insertBatch(yogurtId, 3, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        insertBatch(yogurtId, 4, 0, LocalDate.of(2026, 10, 6), RECEIVED);

        List<Batch> available = batches.findAvailableByProduct(yogurtId);

        assertThat(available).extracting(Batch::getId).containsExactly(sooner, later, noExpiration);
        assertThat(batches.findAllAvailable()).hasSize(3);
    }

    @Test
    void savingADeductionUpdatesTheQuantityAndTheVersion() {
        long batchId = insertBatch(yogurtId, 3, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        Batch batch = batches.findAvailableByProduct(yogurtId).get(0);

        batch.deduct(2);
        batches.saveAll(List.of(batch));

        Batch reloaded = batches.findAvailableByProduct(yogurtId).get(0);
        assertThat(reloaded.getQuantityAvailable()).isEqualTo(1);
        assertThat(reloaded.getVersion()).isEqualTo(batch.getVersion() + 1);
        assertThat(jdbc.queryForObject("select quantity_available from batch where id = ?", Integer.class, batchId))
                .isEqualTo(1);
    }

    @Test
    void savingWithAStaleVersionIsReportedAsAConcurrentUpdate() {
        insertBatch(yogurtId, 3, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        Batch firstCashier = batches.findAvailableByProduct(yogurtId).get(0);
        Batch secondCashier = batches.findAvailableByProduct(yogurtId).get(0);

        firstCashier.deduct(1);
        batches.saveAll(List.of(firstCashier));
        secondCashier.deduct(1);

        assertThatThrownBy(() -> batches.saveAll(List.of(secondCashier)))
                .isInstanceOf(ConcurrentStockUpdateException.class);
    }

    @Test
    void savesASaleWithItsItemsAndCashier() {
        long batchId = insertBatch(yogurtId, 3, 3, LocalDate.of(2026, 10, 8), RECEIVED);
        Sale sale = new Sale(null, "cajero", RECEIVED.plusHours(2), List.of(
                new SaleItem(yogurtId, batchId, 2, new BigDecimal("3200"))));

        Sale saved = sales.save(sale);

        assertThat(saved.id()).isNotNull();
        Sale reloaded = sales.findById(saved.id()).orElseThrow();
        assertThat(reloaded.cashierUsername()).isEqualTo("cajero");
        assertThat(reloaded.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(yogurtId);
            assertThat(item.batchId()).isEqualTo(batchId);
            assertThat(item.quantity()).isEqualTo(2);
            assertThat(item.unitPrice()).isEqualByComparingTo("3200");
        });
        assertThat(jdbc.queryForObject("select total from sale where id = ?", BigDecimal.class, saved.id()))
                .isEqualByComparingTo("6400");
    }

    @Test
    void savingASaleForAnUnknownCashierFails() {
        long batchId = insertBatch(yogurtId, 3, 3, null, RECEIVED);
        Sale sale = new Sale(null, "nadie", RECEIVED, List.of(new SaleItem(yogurtId, batchId, 1, BigDecimal.TEN)));

        assertThatThrownBy(() -> sales.save(sale)).isInstanceOf(IllegalStateException.class);
    }

    private long insertProduct(String barcode, String name, boolean active) {
        jdbc.update("insert into product (barcode, name, sale_price, cost, perishable, safety_stock, active) "
                + "values (?, ?, 3200, 2100, true, 2, ?)", barcode, name, active);
        return jdbc.queryForObject("select id from product where barcode = ?", Long.class, barcode);
    }

    private long insertBatch(long productId, int received, int available, LocalDate expiration, LocalDateTime receivedAt) {
        jdbc.update("insert into batch (product_id, quantity_received, quantity_available, expiration_date, received_at) "
                        + "values (?, ?, ?, ?, ?)",
                productId, received, available, expiration == null ? null : Date.valueOf(expiration),
                Timestamp.valueOf(receivedAt));
        return jdbc.queryForObject("select max(id) from batch", Long.class);
    }
}
