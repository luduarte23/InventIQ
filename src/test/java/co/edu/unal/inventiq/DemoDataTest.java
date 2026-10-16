package co.edu.unal.inventiq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unal.inventiq.domain.rules.GtinValidator;

/**
 * Los datos demo (SQL y el generador de historial en Java) aplicados sobre H2 en memoria:
 * coherentes entre sí, para que la demostración no esconda errores.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:demo-data;DB_CLOSE_DELAY=-1",
        "spring.flyway.locations=classpath:db/migration,classpath:db/demo"})
class DemoDataTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void theCatalogHasTypesBrandsContentAndValidBarcodes() {
        assertThat(count("select count(*) from product")).isEqualTo(21);
        assertThat(count("select count(*) from product where category = 'OTROS'")).isZero();
        assertThat(count("select count(distinct category) from product")).isGreaterThanOrEqualTo(10);
        assertThat(count("select count(*) from product where net_content is null")).isZero();

        GtinValidator gtin = new GtinValidator();
        List<String> barcodes = jdbc.queryForList("select barcode from product", String.class);
        assertThat(barcodes).allSatisfy(code -> assertThat(gtin.problemWith(code)).as(code).isEmpty());
    }

    @Test
    void coffeeIsInTheInventoryWithItsSalePaused() {
        assertThat(jdbc.queryForObject("select sale_enabled from product where barcode = '7703002000049'",
                Boolean.class)).isFalse();
        assertThat(count("select sum(quantity_available) from batch b join product p on p.id = b.product_id "
                + "where p.barcode = '7703002000049'")).isGreaterThan(0);
    }

    @Test
    void thereAreAboutNinetyDaysOfSalesWithTotalsThatMatchTheirItems() {
        assertThat(count("select count(*) from sale")).isGreaterThan(1000);
        assertThat(count("select count(distinct cast(created_at as date)) from sale")).isGreaterThanOrEqualTo(85);
        assertThat(count("select count(*) from sale s where s.total <> "
                + "(select sum(i.quantity * i.unit_price) from sale_item i where i.sale_id = s.id)")).isZero();
    }

    @Test
    void everySoldUnitComesFromADepletedBatchOfTheSameProduct() {
        assertThat(count("select count(*) from sale_item i join batch b on b.id = i.batch_id "
                + "where b.product_id <> i.product_id")).isZero();
        assertThat(count("select count(*) from batch b where b.quantity_available = 0 and b.quantity_received <> "
                + "(select coalesce(sum(i.quantity), 0) from sale_item i where i.batch_id = b.id)")).isZero();
    }

    @Test
    void thePriceHistoryEndsAtTheCurrentPriceOfEveryProduct() {
        assertThat(count("select count(*) from product p where (select count(*) from price_history h "
                + "where h.product_id = p.id) < 4")).isZero();
        assertThat(count("select count(*) from product p where p.sale_price <> (select h.sale_price "
                + "from price_history h where h.product_id = p.id order by h.changed_at desc limit 1)")).isZero();
        assertThat(count("select count(*) from price_history where sale_price < cost")).isZero();
    }

    @Test
    void salesUsedThePriceInForceOnTheirDay() {
        assertThat(count("select count(*) from sale_item i join sale s on s.id = i.sale_id where i.unit_price <> "
                + "(select h.sale_price from price_history h where h.product_id = i.product_id "
                + "and h.changed_at <= s.created_at order by h.changed_at desc limit 1) "
                + "and exists (select 1 from price_history h2 where h2.product_id = i.product_id "
                + "and h2.changed_at <= s.created_at)")).isZero();
    }

    private int count(String sql) {
        Integer value = jdbc.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }
}
