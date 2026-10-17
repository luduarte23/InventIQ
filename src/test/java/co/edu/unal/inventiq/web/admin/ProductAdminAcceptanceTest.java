package co.edu.unal.inventiq.web.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Administración → Productos de punta a punta: escanear o escribir el código, registrar el
 * producto con sus datos, editarlo, pausar su venta y verlo reflejado en Ventas e Inventario.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:admin-products;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class ProductAdminAcceptanceTest {

    private static final String NEW_CODE = "7702001000029";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final LocalDate today = LocalDate.now(ZoneId.of("America/Bogota"));
    private long dairyId;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from sale_item");
        jdbc.update("delete from sale");
        jdbc.update("delete from batch");
        jdbc.update("delete from price_history");
        jdbc.update("delete from product");
        jdbc.update("delete from supplier");
        jdbc.update("insert into supplier (name, lead_time_days, active) values ('Lácteos del Valle', 2, true)");
        jdbc.update("insert into supplier (name, lead_time_days, active) values ('Proveedor retirado', 5, false)");
        dairyId = jdbc.queryForObject("select id from supplier where name = 'Lácteos del Valle'", Long.class);
    }

    // ---- Navegación y catálogo -------------------------------------------------------

    @Test
    void theSidebarOpensTheProductsTab() throws Exception {
        mvc.perform(get("/")).andExpect(content().string(containsString("href=\"/admin/products\"")));
    }

    @Test
    void theProductsTabShowsTheScanFieldTheCatalogAndTheRowButtons() throws Exception {
        long id = insertProduct("7702001000012", "Yogur de fresa");

        mvc.perform(get("/admin/products"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-scanner-input")))
                .andExpect(content().string(containsString("Yogur de fresa")))
                .andExpect(content().string(containsString("Lácteos del Valle")))
                .andExpect(content().string(containsString("/admin/products/" + id + "/edit")))
                .andExpect(content().string(containsString("/admin/products/" + id + "/sale")))
                .andExpect(content().string(containsString("/inventory?q=7702001000012")))
                .andExpect(content().string(containsString("/admin/history?product=" + id)));
    }

    // ---- Registro --------------------------------------------------------------------

    @Test
    void scanningANewCodeOpensTheFormWithTheCodeAlreadyFilledIn() throws Exception {
        mvc.perform(get("/admin/products/new").param("barcode", NEW_CODE))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("value=\"" + NEW_CODE + "\"")))
                .andExpect(content().string(containsString("Lácteos del Valle")))
                .andExpect(content().string(containsString("Granos y cereales")))
                .andExpect(content().string(not(containsString("Proveedor retirado"))));
    }

    @Test
    void aMistypedCodeIsExplainedAndKeptForCorrection() throws Exception {
        mvc.perform(get("/admin/products/new").param("barcode", "7702001000028"))
                .andExpect(view().name("admin/products"))
                .andExpect(content().string(containsString("debería ser 9")))
                .andExpect(content().string(containsString("value=\"7702001000028\"")));
    }

    @Test
    void scanningARegisteredCodeShowsItsDataInsteadOfTheForm() throws Exception {
        insertProduct("7702001000012", "Yogur de fresa");

        mvc.perform(get("/admin/products/new").param("barcode", "7702001000012"))
                .andExpect(view().name("admin/products"))
                .andExpect(content().string(containsString("Ese código ya está registrado")))
                .andExpect(content().string(containsString("Yogur de fresa")));
    }

    @Test
    void anEmptyScanAsksForACode() throws Exception {
        mvc.perform(get("/admin/products/new").param("barcode", "  "))
                .andExpect(content().string(containsString("Escanea o escribe un código")));
    }

    @Test
    void aProductRegisteredWithStockCanBeSoldAtTheRegister() throws Exception {
        mvc.perform(registerProduct("initialQuantity", "12", "initialExpiration", today.plusDays(15).toString()))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("success", containsString("registrado con 12 unidades")));

        assertThat(jdbc.queryForMap("select name, category, brand, net_content, content_unit, description "
                + "from product where barcode = ?", NEW_CODE))
                .containsEntry("NAME", "Leche deslactosada")
                .containsEntry("CATEGORY", "LACTEOS")
                .containsEntry("BRAND", "La Pradera")
                .containsEntry("CONTENT_UNIT", "L")
                .containsEntry("DESCRIPTION", "Leche deslactosada en bolsa.");
        assertThat(jdbc.queryForObject("select quantity_available from batch", Integer.class)).isEqualTo(12);
        assertThat(jdbc.queryForObject("select expiration_date from batch", LocalDate.class))
                .isEqualTo(today.plusDays(15));
        assertThat(count("price_history")).isEqualTo(1);

        MockHttpSession cashier = new MockHttpSession();
        mvc.perform(post("/pos/scan").session(cashier).param("barcode", NEW_CODE));
        mvc.perform(post("/pos/close").session(cashier))
                .andExpect(flash().attribute("success", containsString("1 unidad")));
        assertThat(jdbc.queryForObject("select quantity_available from batch", Integer.class)).isEqualTo(11);
    }

    @Test
    void theNewDetailsAppearInTheCatalogAndInTheInventory() throws Exception {
        mvc.perform(registerProduct("initialQuantity", "6", "initialExpiration", today.plusDays(3).toString()));

        for (String page : new String[] {"/admin/products", "/inventory"}) {
            mvc.perform(get(page))
                    .andExpect(content().string(containsString("Leche deslactosada")))
                    .andExpect(content().string(containsString("Lácteos")))
                    .andExpect(content().string(containsString("La Pradera")))
                    .andExpect(content().string(containsString("1,5 L")))
                    .andExpect(content().string(containsString("Leche deslactosada en bolsa.")))
                    .andExpect(content().string(containsString("exp-soon")));
        }
    }

    @Test
    void aProductCanBeRegisteredWithoutStockOrWithItsSalePaused() throws Exception {
        mvc.perform(registerProduct())
                .andExpect(flash().attribute("success", containsString("sin stock inicial")));
        mvc.perform(registerProduct("barcode", "7703002000018", "name", "Arroz", "perishable", "false",
                        "saleEnabled", "false", "initialQuantity", "50"))
                .andExpect(flash().attribute("success", containsString("con la venta pausada")));

        assertThat(count("product")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select sale_enabled from product where barcode = '7703002000018'",
                Boolean.class)).isFalse();
    }

    @Test
    void invalidDataShowsEveryProblemAndSavesNothing() throws Exception {
        mvc.perform(registerProduct("salePrice", "3900", "cost", "5200", "initialQuantity", "5", "category", "",
                        "contentUnit", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("Revisa si quedaron invertidos")))
                .andExpect(content().string(containsString("necesita la fecha de vencimiento")))
                .andExpect(content().string(containsString("Elige el tipo de producto")))
                .andExpect(content().string(containsString("Elige la unidad del contenido")));

        assertThat(count("product")).isZero();
    }

    @Test
    void unreadableNumbersGetAFriendlyMessageAndNothingIsSaved() throws Exception {
        mvc.perform(registerProduct("salePrice", "cinco mil", "initialQuantity", "doce", "name", ""))
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("Escribe solo números")))
                .andExpect(content().string(containsString("Escribe un número entero")))
                .andExpect(content().string(containsString("Escribe el nombre del producto")));

        assertThat(count("product")).isZero();
    }

    @Test
    void aThousandsSeparatorThatArrivesAsDecimalsIsCaught() throws Exception {
        mvc.perform(registerProduct("salePrice", "5.200", "cost", "3.900"))
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("pesos enteros")));

        assertThat(count("product")).isZero();
    }

    @Test
    void aDuplicateCodeIsRejected() throws Exception {
        insertProduct(NEW_CODE, "Leche entera");

        mvc.perform(registerProduct())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("Ya existe un producto con el código")));

        assertThat(count("product")).isEqualTo(1);
    }

    // ---- Edición ---------------------------------------------------------------------

    @Test
    void editingShowsTheCurrentDataAndSavesTheChangesWithTheNewPriceInTheHistory() throws Exception {
        mvc.perform(registerProduct());
        long id = jdbc.queryForObject("select id from product where barcode = ?", Long.class, NEW_CODE);

        mvc.perform(get("/admin/products/" + id + "/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Editar producto")))
                .andExpect(content().string(containsString("value=\"Leche deslactosada\"")))
                .andExpect(content().string(containsString("value=\"5200\"")));

        mvc.perform(editProduct(id, "name", "Leche sin lactosa", "salePrice", "5600", "cost", "4100"))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("success", containsString("El nuevo precio quedó en el historial")));

        assertThat(jdbc.queryForObject("select name from product where id = ?", String.class, id))
                .isEqualTo("Leche sin lactosa");
        assertThat(jdbc.queryForObject("select barcode from product where id = ?", String.class, id))
                .isEqualTo(NEW_CODE);
        assertThat(count("price_history")).isEqualTo(2);
    }

    @Test
    void anInvalidEditShowsTheErrorsAndKeepsTheProduct() throws Exception {
        mvc.perform(registerProduct());
        long id = jdbc.queryForObject("select id from product where barcode = ?", Long.class, NEW_CODE);

        mvc.perform(editProduct(id, "salePrice", "1000", "netContentAmount", "6.5", "contentUnit", "UND"))
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("menor que el costo")))
                .andExpect(content().string(containsString("se cuentan enteras")));

        assertThat(jdbc.queryForObject("select sale_price from product where id = ?", Integer.class, id))
                .isEqualTo(5200);
    }

    @Test
    void editingAProductThatDoesNotExistGoesBackToTheList() throws Exception {
        mvc.perform(get("/admin/products/999999/edit"))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("error", containsString("no existe")));
        mvc.perform(editProduct(999999, "name", "X"))
                .andExpect(redirectedUrl("/admin/products"));
    }

    // ---- Venta pausada ---------------------------------------------------------------

    @Test
    void pausingTheSaleKeepsTheProductInTheInventoryButThePosRefusesIt() throws Exception {
        mvc.perform(registerProduct("initialQuantity", "10", "initialExpiration", today.plusDays(30).toString()));
        long id = jdbc.queryForObject("select id from product where barcode = ?", Long.class, NEW_CODE);

        mvc.perform(post("/admin/products/" + id + "/sale").param("enabled", "false"))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("success", containsString("Sigue en el inventario")));

        MockHttpSession cashier = new MockHttpSession();
        mvc.perform(post("/pos/scan").session(cashier).param("barcode", NEW_CODE))
                .andExpect(flash().attribute("error", containsString("no está a la venta")));
        mvc.perform(get("/inventory"))
                .andExpect(content().string(containsString("Venta pausada")))
                .andExpect(content().string(containsString("Leche deslactosada")));

        mvc.perform(post("/admin/products/" + id + "/sale").param("enabled", "true"))
                .andExpect(flash().attribute("success", containsString("ya se puede vender")));
        mvc.perform(post("/pos/scan").session(cashier).param("barcode", NEW_CODE))
                .andExpect(flash().attributeExists("lastScanned"));
    }

    @Test
    void pausingAProductThatDoesNotExistShowsAnError() throws Exception {
        mvc.perform(post("/admin/products/999999/sale").param("enabled", "false"))
                .andExpect(redirectedUrl("/admin/products"))
                .andExpect(flash().attribute("error", containsString("no existe")));
    }

    /**
     * POST de un producto válido. {@code overrides} son pares campo, valor que reemplazan
     * los de por defecto (MockMvc.param agrega valores en lugar de reemplazarlos).
     */
    private MockHttpServletRequestBuilder registerProduct(String... overrides) {
        Map<String, String> fields = validFields();
        fields.put("barcode", NEW_CODE);
        return withFields(post("/admin/products"), fields, overrides);
    }

    private MockHttpServletRequestBuilder editProduct(long id, String... overrides) {
        return withFields(post("/admin/products/" + id), validFields(), overrides);
    }

    private Map<String, String> validFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("name", "Leche deslactosada");
        fields.put("category", "LACTEOS");
        fields.put("brand", "La Pradera");
        fields.put("netContentAmount", "1.5");
        fields.put("contentUnit", "L");
        fields.put("description", "Leche deslactosada en bolsa.");
        fields.put("salePrice", "5200");
        fields.put("cost", "3900");
        fields.put("perishable", "true");
        fields.put("supplierId", String.valueOf(dairyId));
        fields.put("safetyStock", "6");
        return fields;
    }

    private static MockHttpServletRequestBuilder withFields(MockHttpServletRequestBuilder request,
                                                            Map<String, String> fields, String... overrides) {
        for (int i = 0; i < overrides.length; i += 2) {
            fields.put(overrides[i], overrides[i + 1]);
        }
        fields.forEach((field, value) -> request.param(field, value));
        return request;
    }

    private long insertProduct(String barcode, String name) {
        jdbc.update("insert into product (barcode, name, sale_price, cost, perishable, supplier_id, safety_stock) "
                + "values (?, ?, 3200, 2100, true, ?, 4)", barcode, name, dairyId);
        return jdbc.queryForObject("select id from product where barcode = ?", Long.class, barcode);
    }

    private int count(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }
}
