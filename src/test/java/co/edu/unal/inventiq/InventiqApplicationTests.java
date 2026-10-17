package co.edu.unal.inventiq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Prueba de humo: el contexto arranca, Flyway aplica las migraciones y Hibernate
 * las valida (ddl-auto=validate). Usa H2 en memoria para no tocar ./data.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:inventiq-test;DB_CLOSE_DELAY=-1")
class InventiqApplicationTests {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void migrationsCreateTheInitialSchema() {
        List<String> tables = jdbc.queryForList(
                "SELECT LOWER(table_name) FROM information_schema.tables WHERE table_schema = 'PUBLIC'",
                String.class);

        assertThat(tables).contains(
                "app_user", "supplier", "product", "batch", "sale", "sale_item",
                "purchase_order", "order_line", "settings", "audit_log", "alert");
    }

    @Test
    void settingsHasItsSingleDefaultRow() {
        Integer window = jdbc.queryForObject("SELECT sales_window_days FROM settings WHERE id = 1", Integer.class);

        assertThat(window).isEqualTo(30);
    }
}
