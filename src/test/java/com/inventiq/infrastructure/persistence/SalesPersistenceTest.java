package com.inventiq.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.SaleItem;
import com.inventiq.infrastructure.persistence.adapter.BatchRepositoryAdapter;
import com.inventiq.infrastructure.persistence.adapter.ProductRepositoryAdapter;
import com.inventiq.infrastructure.persistence.adapter.SaleRepositoryAdapter;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba la persistencia (B3) contra H2 con las tablas reales de Flyway.
 * @DataJpaTest levanta solo JPA y deshace los cambios al final de cada prueba.
 */
@DataJpaTest
@Import({ProductRepositoryAdapter.class, BatchRepositoryAdapter.class, SaleRepositoryAdapter.class})
class SalesPersistenceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final Instant NOW = Instant.parse("2026-10-05T15:00:00Z");

    @Autowired private ProductRepositoryAdapter products;
    @Autowired private BatchRepositoryAdapter batches;
    @Autowired private SaleRepositoryAdapter sales;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    private Long productId;

    @BeforeEach
    void setUp() {
        productId = products.save(new Product(null, "7702001000012", "Yogur de fresa", new BigDecimal("3200"),
                new BigDecimal("2100"), true, null, 0, 0, true, null)).getId();
    }

    @Test
    void savesReadsAndDeletesASaleWithItsItems() {
        // La venta exige un cajero que exista en la tabla users.
        jdbc.update("insert into users (username, password_hash, role) values ('cajero', 'x', 'CASHIER')");
        Long cashierId = jdbc.queryForObject("select id from users", Long.class);
        Batch batch = batches.save(batch(3, null));
        Sale sale = Sale.open(cashierId, NOW);
        sale.addItem(new SaleItem(null, null, productId, batch.getId(), 2, new BigDecimal("3200"), BigDecimal.ZERO));

        Long saleId = sales.save(sale).getId();
        reloadFromDatabase();
        Sale read = sales.findById(saleId).get();

        assertEquals(1, read.getItems().size());
        assertEquals(new BigDecimal("6400.00"), read.getTotal());

        sales.delete(saleId);
        reloadFromDatabase();
        assertTrue(sales.findById(saleId).isEmpty());
        assertEquals(0, jdbc.queryForObject("select count(*) from sale_items", Integer.class));
    }

    @Test
    void availableBatchesComeInFifoOrderWithoutExpiredOnes() {
        Batch noExpiration = batches.save(batch(5, null));
        Batch later = batches.save(batch(3, TODAY.plusDays(25)));
        Batch sooner = batches.save(batch(3, TODAY.plusDays(3)));
        batches.save(batch(4, TODAY.minusDays(1)));   // vencido: no debe aparecer
        reloadFromDatabase();

        List<Batch> available = batches.findAvailableByProduct(productId, TODAY);

        assertEquals(List.of(sooner, later, noExpiration), available);
    }

    /** Escribe en la base lo pendiente y olvida lo que JPA tenía en memoria, para leer de verdad de H2. */
    private void reloadFromDatabase() {
        entityManager.flush();
        entityManager.clear();
    }

    private Batch batch(int quantity, LocalDate expiration) {
        return new Batch(null, productId, quantity, quantity, expiration, NOW,
                new BigDecimal("2100"), BigDecimal.ZERO, null, null);
    }
}
