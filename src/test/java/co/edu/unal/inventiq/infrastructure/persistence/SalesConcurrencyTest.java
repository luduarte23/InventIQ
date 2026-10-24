package co.edu.unal.inventiq.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unal.inventiq.application.sales.SalesService;
import co.edu.unal.inventiq.domain.exception.InsufficientStockException;
import co.edu.unal.inventiq.domain.model.Cart;

/**
 * Concurrencia (B7): dos cajeros cierran ventas del mismo lote al mismo tiempo contra
 * H2 real. Con bloqueo optimista y reintento el stock cuadra y nunca queda negativo.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sales-concurrency;DB_CLOSE_DELAY=-1")
class SalesConcurrencyTest {

    @Autowired
    private SalesService sales;

    @Autowired
    private JdbcTemplate jdbc;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private long batchId;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from sale_item");
        jdbc.update("delete from sale");
        jdbc.update("delete from batch");
        jdbc.update("delete from product");
        jdbc.update("insert into product (barcode, name, sale_price, cost, perishable, safety_stock) "
                + "values ('7703002000017', 'Arroz 1 kg', 4800, 3600, false, 0)");
        long productId = jdbc.queryForObject("select id from product", Long.class);
        jdbc.update("insert into batch (product_id, quantity_received, quantity_available, received_at) "
                + "values (?, 10, 10, ?)", productId, Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        batchId = jdbc.queryForObject("select id from batch", Long.class);
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @RepeatedTest(5)
    void twoSimultaneousSalesThatFitAreBothRecorded() throws Exception {
        List<Outcome> outcomes = closeSimultaneously(5, 5);

        assertThat(outcomes).containsOnly(Outcome.SOLD);
        assertThat(stock()).isZero();
        assertThat(soldUnits()).isEqualTo(10);
    }

    @RepeatedTest(5)
    void whenBothDoNotFitOneSaleIsRejectedAndStockNeverGoesNegative() throws Exception {
        List<Outcome> outcomes = closeSimultaneously(6, 6);

        assertThat(outcomes).containsExactlyInAnyOrder(Outcome.SOLD, Outcome.INSUFFICIENT_STOCK);
        assertThat(stock()).isEqualTo(4);
        assertThat(soldUnits()).isEqualTo(6);
    }

    private List<Outcome> closeSimultaneously(int first, int second) throws Exception {
        Cart cartA = sales.scan(sales.startSale(), "7703002000017", first);
        Cart cartB = sales.scan(sales.startSale(), "7703002000017", second);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Outcome>> futures = new ArrayList<>();
        for (Cart cart : List.of(cartA, cartB)) {
            Callable<Outcome> task = () -> {
                start.await();
                try {
                    sales.close(cart, "cajero");
                    return Outcome.SOLD;
                } catch (InsufficientStockException e) {
                    return Outcome.INSUFFICIENT_STOCK;
                }
            };
            futures.add(pool.submit(task));
        }
        start.countDown();

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> future : futures) {
            outcomes.add(future.get(30, TimeUnit.SECONDS));
        }
        return outcomes;
    }

    private int stock() {
        return jdbc.queryForObject("select quantity_available from batch where id = ?", Integer.class, batchId);
    }

    private int soldUnits() {
        return jdbc.queryForObject("select coalesce(sum(quantity), 0) from sale_item", Integer.class);
    }

    private enum Outcome { SOLD, INSUFFICIENT_STOCK }
}
