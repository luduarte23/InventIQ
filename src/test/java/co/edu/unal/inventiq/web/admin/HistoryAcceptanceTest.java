package co.edu.unal.inventiq.web.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** Administración → Historial con H2 real: indicadores, gráficas, historial de ventas y precios. */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:history;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class HistoryAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
    private long milkId;
    private long batchId;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from sale_item");
        jdbc.update("delete from sale");
        jdbc.update("delete from batch");
        jdbc.update("delete from price_history");
        jdbc.update("delete from product");
        jdbc.update("insert into product (barcode, name, category, sale_price, cost, perishable, safety_stock) "
                + "values ('7702001000029', 'Leche entera', 'LACTEOS', 4500, 3300, true, 0)");
        milkId = jdbc.queryForObject("select id from product", Long.class);
        jdbc.update("insert into batch (product_id, quantity_received, quantity_available, received_at) "
                + "values (?, 20, 0, ?)", milkId, Timestamp.valueOf(today.minusDays(30).atStartOfDay()));
        batchId = jdbc.queryForObject("select id from batch", Long.class);
        jdbc.update("insert into price_history (product_id, sale_price, cost, changed_at) values (?, 4000, 3000, ?)",
                milkId, Timestamp.valueOf(today.minusDays(60).atTime(8, 0)));
        jdbc.update("insert into price_history (product_id, sale_price, cost, changed_at) values (?, 4500, 3300, ?)",
                milkId, Timestamp.valueOf(today.minusDays(10).atTime(8, 0)));
    }

    @Test
    void withoutSalesTheSalesSectionShowsAnEmptyStateButThePricesAreThere() throws Exception {
        mvc.perform(get("/admin/history"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aún no hay ventas en este período")))
                .andExpect(content().string(containsString("Precios de compra y venta")))
                .andExpect(content().string(containsString("Leche entera")));
    }

    @Test
    void withSalesItShowsIndicatorsChartsAndTheSalesHistory() throws Exception {
        sell(today.minusDays(1).atTime(9, 15), 2, 4500);

        mvc.perform(get("/admin/history").param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/history"))
                .andExpect(model().attribute("period", 7))
                .andExpect(content().string(containsString("Margen bruto estimado")))
                .andExpect(content().string(containsString("data-chart=\"trend\"")))
                .andExpect(content().string(containsString("data-chart=\"performance\"")))
                .andExpect(content().string(containsString("Historial de ventas")))
                .andExpect(content().string(containsString("$ 9.000")))
                .andExpect(content().string(containsString("window.inventiqHistory")))
                .andExpect(content().string(containsString("\"points\"")))
                .andExpect(content().string(containsString("/webjars/chart.js/4.5.1/dist/chart.umd.min.js")));
    }

    @Test
    void theProductOfThePriceChartCanBeChosenFromTheLink() throws Exception {
        mvc.perform(get("/admin/history").param("product", String.valueOf(milkId)))
                .andExpect(model().attribute("selectedProduct", milkId));
    }

    @Test
    void anUnknownPeriodFallsBackToThirtyDays() throws Exception {
        mvc.perform(get("/admin/history").param("days", "45"))
                .andExpect(model().attribute("period", 30));
    }

    @Test
    void theChartLibraryIsServedFromTheJarWithoutInternet() throws Exception {
        mvc.perform(get("/webjars/chart.js/4.5.1/dist/chart.umd.min.js"))
                .andExpect(status().isOk());
        mvc.perform(get("/js/history-charts.js")).andExpect(status().isOk());
    }

    private void sell(LocalDateTime at, int quantity, int price) {
        long cashierId = jdbc.queryForObject("select id from app_user where username = 'cajero'", Long.class);
        jdbc.update("insert into sale (cashier_id, created_at, total) values (?, ?, ?)", cashierId,
                Timestamp.valueOf(at), quantity * price);
        long saleId = jdbc.queryForObject("select max(id) from sale", Long.class);
        jdbc.update("insert into sale_item (sale_id, product_id, batch_id, quantity, unit_price) values (?, ?, ?, ?, ?)",
                saleId, milkId, batchId, quantity, price);
    }
}
