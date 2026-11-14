package com.inventiq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.inventiq.domain.port.SettingsRepository;
import java.math.BigDecimal;
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

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }
}
