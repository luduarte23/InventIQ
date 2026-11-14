package com.inventiq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.inventiq.domain.port.SaleRepository;
import com.inventiq.domain.port.SettingsRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Carga las migraciones normales MÁS los datos demo (db/demo) sobre una base en memoria
 * propia, igual que el perfil "demo". Si el script demo deja de funcionar con el esquema
 * actual, esta prueba falla en el CI antes de que alguien lo descubra en una presentación.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventiqdemo;DB_CLOSE_DELAY=-1",
        "spring.flyway.locations=classpath:db/migration,classpath:db/demo"})
class DemoDataTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private SettingsRepository settings;
    @Autowired private SaleRepository sales;

    @Test
    void theDemoScriptRunsOnTopOfTheCurrentSchema() {
        assertEquals(1, jdbc.queryForObject(
                "select count(*) from users where username = 'cajero'", Integer.class));
        assertEquals(new BigDecimal("500000.00"), settings.get().getWeeklyBudget());
    }

    @Test
    void theDemoCatalogHasSuppliersAndProducts() {
        assertEquals(5, count("select count(*) from suppliers"));
        assertEquals(96, count("select count(*) from products"));
        // Tres productos sin proveedor, para el pedido "por revisar".
        assertEquals(3, count("select count(*) from products where supplier_id is null"));
        // Ningún producto se vende por debajo de su costo.
        assertEquals(0, count("select count(*) from products where sale_price < unit_cost"));
    }

    @Test
    void everyProductHasStockAndSomeBatchesAreAboutToExpire() {
        // Todos los productos tienen al menos un lote vendible: con unidades y sin vencer.
        assertEquals(0, count("select count(*) from products p where not exists ("
                + "select 1 from batches b where b.product_id = p.id and b.quantity_remaining > 0"
                + " and (b.expiration_date is null or b.expiration_date >= current_date))"));
        // Hay lotes por vencer en los próximos 7 días y dos lotes vencidos que aún tienen unidades.
        assertTrue(count("select count(*) from batches where expiration_date"
                + " between current_date and dateadd('DAY', 7, current_date)") > 0);
        assertEquals(2, count("select count(*) from batches"
                + " where expiration_date < current_date and quantity_remaining > 0"));
    }

    @Test
    void thereAreNinetyDaysOfClosedSalesToCalculateTheVelocity() {
        // Solo ventas cerradas, repartidas en los últimos 90 días.
        assertEquals(0, count("select count(*) from sales where closed_at is null"));
        assertEquals(90, count("select count(distinct cast(created_at as date)) from sales"));
        // El total de cada venta es la suma de sus líneas.
        assertEquals(0, count("select count(*) from sales s where s.total <> ("
                + "select sum(i.quantity * i.unit_price) from sale_items i where i.sale_id = s.id)"));

        // La leche entera Alpina (1001) vende unas 6 unidades por día: cerca de 180 en 30 días.
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        int milk = sales.unitsSoldSince(productId("1001"), thirtyDaysAgo);
        assertTrue(milk >= 165 && milk <= 195, "unidades de leche en 30 días: " + milk);
        // El Chocolisto (2008) no tiene historial a propósito.
        assertEquals(0, sales.unitsSoldSince(productId("2008"), thirtyDaysAgo));
    }

    private long productId(String barcode) {
        return jdbc.queryForObject("select id from products where barcode = ?", Long.class, barcode);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
