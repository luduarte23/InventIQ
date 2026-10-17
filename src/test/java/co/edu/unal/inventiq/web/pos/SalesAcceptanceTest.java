package co.edu.unal.inventiq.web.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * Prueba de aceptación de US-03 de punta a punta (B8): pantalla de ventas, servicio,
 * FIFO, H2 con el esquema real y redirecciones, tal como lo usa el cajero.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:sales-acceptance;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@WithMockUser(username = "cajero", roles = "CASHIER")
class SalesAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final MockHttpSession session = new MockHttpSession();
    /** "Hoy" en la zona de la tienda, igual que el Clock de la aplicación (el CI corre en UTC). */
    private final LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));

    private long batchExpiringFirst;
    private long batchExpiringLater;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from sale_item");
        jdbc.update("delete from sale");
        jdbc.update("delete from batch");
        jdbc.update("delete from price_history");
        jdbc.update("delete from product");

        // US-03: 6 unidades, 3 que vencen antes y 3 que vencen después.
        long yogurt = insertProduct("7702001000011", "Yogur de fresa", 3200);
        batchExpiringFirst = insertBatch(yogurt, 3, today.plusDays(3));
        batchExpiringLater = insertBatch(yogurt, 3, today.plusDays(25));
    }

    @Test
    void us03Scenario1_twoScansDeductTwoUnitsFromTheBatchThatExpiresFirst() throws Exception {
        scan("7702001000011");
        scan("7702001000011");

        mvc.perform(get("/pos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("Yogur de fresa")))
                .andExpect(content().string(Matchers.containsString("$ 6.400")));

        mvc.perform(post("/pos/close").with(csrf()).session(session))
                .andExpect(redirectedUrl("/pos"))
                .andExpect(flash().attribute("success", Matchers.containsString("2 unidades")));

        assertThat(quantityOf(batchExpiringFirst)).isEqualTo(1);
        assertThat(quantityOf(batchExpiringLater)).isEqualTo(3);
        assertThat(count("sale")).isEqualTo(1);
        assertThat(jdbc.queryForObject("select batch_id from sale_item", Long.class)).isEqualTo(batchExpiringFirst);
    }

    @Test
    void us03Scenario2_unknownBarcodeShowsAMessageAndTheCashierCanContinue() throws Exception {
        scan("7702001000011");

        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", "9999999999999"))
                .andExpect(redirectedUrl("/pos"))
                .andExpect(flash().attribute("error", Matchers.containsString("no está registrado")));

        assertThat(quantityOf(batchExpiringFirst)).isEqualTo(3);
        assertThat(count("sale")).isZero();

        mvc.perform(post("/pos/close").with(csrf()).session(session))
                .andExpect(flash().attribute("success", Matchers.containsString("1 unidad,")));
        assertThat(quantityOf(batchExpiringFirst)).isEqualTo(2);
    }

    @Test
    void aCashierRegistersTenProductsWithTheKeyboardOnly() throws Exception {
        for (int i = 0; i < 10; i++) {
            long product = insertProduct("770900000000" + i, "Producto " + i, 1000);
            insertBatch(product, 5, null);
        }

        for (int i = 0; i < 10; i++) {
            scan("770900000000" + i);
        }
        mvc.perform(post("/pos/close").with(csrf()).session(session))
                .andExpect(flash().attribute("success", Matchers.containsString("10 unidades")));

        assertThat(count("sale_item")).isEqualTo(10);
    }

    @Test
    void emptyEnterDoesNothingAndCancelLeavesTheStockUntouched() throws Exception {
        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", "   "))
                .andExpect(redirectedUrl("/pos"))
                .andExpect(flash().attributeCount(0));

        scan("7702001000011");
        mvc.perform(post("/pos/cancel").with(csrf()).session(session))
                .andExpect(flash().attribute("success", Matchers.containsString("cancelada")));

        assertThat(quantityOf(batchExpiringFirst)).isEqualTo(3);
        mvc.perform(post("/pos/close").with(csrf()).session(session))
                .andExpect(flash().attribute("error", Matchers.containsString("No hay productos")));
    }

    @Test
    void invalidQuantityShowsAMessage() throws Exception {
        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", "7702001000011").param("quantity", "dos"))
                .andExpect(flash().attribute("error", Matchers.containsString("número entero")));
        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", "7702001000011").param("quantity", "0"))
                .andExpect(flash().attribute("error", Matchers.containsString("mayor que cero")));
    }

    @Test
    void removingALineTakesItOutOfTheSale() throws Exception {
        long yogurtId = jdbc.queryForObject("select id from product where barcode = '7702001000011'", Long.class);
        scan("7702001000011");

        mvc.perform(post("/pos/remove/" + yogurtId).with(csrf()).session(session)).andExpect(redirectedUrl("/pos"));

        mvc.perform(get("/pos").session(session))
                .andExpect(content().string(Matchers.containsString("Aún no hay productos")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN") // el inventario es solo del administrador
    void inventoryShowsBatchesInFifoOrder() throws Exception {
        String html = mvc.perform(get("/inventory"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Yogur de fresa").contains("Vence en 3 días");
        int first = html.indexOf("data-lot=\"" + batchExpiringFirst + "\"");
        int later = html.indexOf("data-lot=\"" + batchExpiringLater + "\"");
        assertThat(first).isPositive().isLessThan(later);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN") // el inventario es solo del administrador
    void aProductWithPausedSaleIsRejectedAtTheRegisterButStaysInTheInventory() throws Exception {
        jdbc.update("update product set sale_enabled = false where barcode = '7702001000011'");

        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", "7702001000011"))
                .andExpect(flash().attribute("error", Matchers.containsString("no está a la venta")));

        mvc.perform(get("/inventory"))
                .andExpect(content().string(Matchers.containsString("Venta pausada")))
                .andExpect(content().string(Matchers.containsString("Yogur de fresa")));
        assertThat(quantityOf(batchExpiringFirst)).isEqualTo(3);
    }

    private void scan(String barcode) throws Exception {
        mvc.perform(post("/pos/scan").with(csrf()).session(session).param("barcode", barcode))
                .andExpect(redirectedUrl("/pos"))
                .andExpect(flash().attributeCount(1))
                .andExpect(flash().attributeExists("lastScanned"));
    }

    private long insertProduct(String barcode, String name, int price) {
        jdbc.update("insert into product (barcode, name, sale_price, cost, perishable, safety_stock) "
                + "values (?, ?, ?, ?, true, 0)", barcode, name, price, price / 2);
        return jdbc.queryForObject("select id from product where barcode = ?", Long.class, barcode);
    }

    private long insertBatch(long productId, int quantity, LocalDate expiration) {
        jdbc.update("insert into batch (product_id, quantity_received, quantity_available, expiration_date, received_at) "
                        + "values (?, ?, ?, ?, ?)",
                productId, quantity, quantity, expiration == null ? null : Date.valueOf(expiration),
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
        return jdbc.queryForObject("select max(id) from batch", Long.class);
    }

    private int quantityOf(long batchId) {
        return jdbc.queryForObject("select quantity_available from batch where id = ?", Integer.class, batchId);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
