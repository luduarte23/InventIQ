package co.edu.unal.inventiq.application.admin;

import static co.edu.unal.inventiq.fixtures.NewProductBuilder.aValidProduct;
import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.application.sales.SalesService;
import co.edu.unal.inventiq.domain.exception.InvalidProductException;
import co.edu.unal.inventiq.domain.exception.ProductNotFoundException;
import co.edu.unal.inventiq.domain.exception.ProductNotForSaleException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.Cart;
import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.fakes.DirectTransactionRunner;
import co.edu.unal.inventiq.fakes.InMemoryBatchRepository;
import co.edu.unal.inventiq.fakes.InMemoryPriceHistoryRepository;
import co.edu.unal.inventiq.fakes.InMemoryProductRepository;
import co.edu.unal.inventiq.fakes.InMemorySaleRepository;
import co.edu.unal.inventiq.fakes.InMemorySupplierRepository;

/** Catálogo con fakes: sin Spring y sin base de datos. */
class CatalogServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 16, 10, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    private InMemoryProductRepository products;
    private InMemoryBatchRepository batches;
    private InMemorySupplierRepository suppliers;
    private InMemoryPriceHistoryRepository priceHistory;
    private DirectTransactionRunner transactions;
    private Clock clock;
    private CatalogService catalog;

    private Supplier dairy;
    private Supplier retired;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductRepository();
        batches = new InMemoryBatchRepository();
        suppliers = new InMemorySupplierRepository();
        priceHistory = new InMemoryPriceHistoryRepository();
        transactions = new DirectTransactionRunner();
        clock = Clock.fixed(NOW.atZone(BOGOTA).toInstant(), BOGOTA);
        catalog = new CatalogService(products, batches, suppliers, priceHistory, transactions, clock);

        dairy = suppliers.add("Lácteos del Valle", true);
        retired = suppliers.add("Proveedor retirado", false);
    }

    // ---- Búsqueda por código ----------------------------------------------------------

    @Test
    void aNewValidCodeIsAvailableForRegistration() {
        assertThat(catalog.lookup(" 7702001000029 ")).isEqualTo(new BarcodeLookup.Available("7702001000029"));
    }

    @Test
    void aMistypedCodeIsExplained() {
        BarcodeLookup result = catalog.lookup("7702001000028");

        assertThat(result).isInstanceOfSatisfying(BarcodeLookup.Invalid.class,
                invalid -> assertThat(invalid.reason()).contains("debería ser 9"));
    }

    @Test
    void aRegisteredCodeShowsTheProductAndItsSupplier() {
        catalog.register(aValidProduct().supplierId(dairy.id()).build());

        BarcodeLookup result = catalog.lookup("7702001000029");

        assertThat(result).isInstanceOfSatisfying(BarcodeLookup.AlreadyRegistered.class, registered -> {
            assertThat(registered.entry().product().name()).isEqualTo("Leche deslactosada");
            assertThat(registered.entry().supplierName()).isEqualTo("Lácteos del Valle");
        });
    }

    @Test
    void productsWithOldNonStandardCodesAreStillFound() {
        products.add("123", "Producto antiguo", 1000);

        assertThat(catalog.lookup("123")).isInstanceOf(BarcodeLookup.AlreadyRegistered.class);
    }

    // ---- Registro --------------------------------------------------------------------

    @Test
    void registeringSavesTheProductWithItsDetailsAndOpensItsPriceHistory() {
        Product saved = catalog.register(aValidProduct().supplierId(dairy.id()).brand("  La Pradera ")
                .netContent("1.5", ContentUnit.L).description("  ").build());

        assertThat(saved.id()).isNotNull();
        assertThat(products.findByBarcode("7702001000029")).contains(saved);
        assertThat(saved.category()).isEqualTo(ProductCategory.LACTEOS);
        assertThat(saved.brand()).isEqualTo("La Pradera");
        assertThat(saved.netContent().display()).isEqualTo("1,5 L");
        assertThat(saved.description()).isNull();
        assertThat(saved.active()).isTrue();
        assertThat(saved.saleEnabled()).isTrue();
        assertThat(saved.reorderPoint()).isNull();
        assertThat(batches.all()).isEmpty();
        assertThat(priceHistory.findByProduct(saved.id())).singleElement().satisfies(change -> {
            assertThat(change.salePrice()).isEqualByComparingTo("5200");
            assertThat(change.cost()).isEqualByComparingTo("3900");
            assertThat(change.changedAt()).isEqualTo(NOW);
        });
        assertThat(transactions.executions()).isEqualTo(1);
    }

    @Test
    void aProductCanBeRegisteredWithItsSalePaused() {
        Product saved = catalog.register(aValidProduct().saleEnabled(false).initialStock(40, TODAY.plusDays(60)).build());

        assertThat(saved.saleEnabled()).isFalse();
        assertThat(batches.all()).hasSize(1);
    }

    @Test
    void initialStockBecomesTheFirstBatch() {
        Product saved = catalog.register(aValidProduct().initialStock(12, TODAY.plusDays(15)).build());

        assertThat(batches.all()).singleElement().satisfies(batch -> {
            assertThat(batch.getProductId()).isEqualTo(saved.id());
            assertThat(batch.getQuantityReceived()).isEqualTo(12);
            assertThat(batch.getQuantityAvailable()).isEqualTo(12);
            assertThat(batch.getExpirationDate()).isEqualTo(TODAY.plusDays(15));
            assertThat(batch.getReceivedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void aRegisteredProductWithStockCanBeSoldRightAway() {
        catalog.register(aValidProduct().perishable(false).initialStock(5, null).build());
        SalesService sales = new SalesService(products, batches, new InMemorySaleRepository(), transactions, clock);

        Cart cart = sales.scan(sales.startSale(), "7702001000029", 2);
        sales.close(cart, "cajero");

        assertThat(batches.all()).extracting(Batch::getQuantityAvailable).containsExactly(3);
    }

    @Test
    void aDuplicateCodeNamesTheProductThatAlreadyHasIt() {
        products.add("7702001000029", "Leche entera", 4500);

        assertThatThrownBy(() -> catalog.register(aValidProduct().build()))
                .isInstanceOfSatisfying(InvalidProductException.class, e -> assertThat(e.getFieldErrors())
                        .containsOnlyKeys("barcode")
                        .hasEntrySatisfying("barcode", message -> assertThat(message).contains("Leche entera")));
        assertThat(products.count()).isEqualTo(1);
    }

    @Test
    void theSupplierMustExistAndBeActive() {
        assertThat(catalog.validate(aValidProduct().supplierId(retired.id()).build())).containsOnlyKeys("supplierId");
        assertThat(catalog.validate(aValidProduct().supplierId(99L).build())).containsOnlyKeys("supplierId");
        assertThat(catalog.validate(aValidProduct().supplierId(null).build())).isEmpty();
    }

    @Test
    void allProblemsAreReportedTogetherAndNothingIsSaved() {
        var invalid = aValidProduct().name("").category(null).salePrice("3000").cost("4500")
                .initialStock(5, null).build();

        assertThatThrownBy(() -> catalog.register(invalid))
                .isInstanceOfSatisfying(InvalidProductException.class, e -> assertThat(e.getFieldErrors())
                        .containsOnlyKeys("name", "category", "salePrice", "initialExpiration"));
        assertThat(products.count()).isZero();
        assertThat(batches.all()).isEmpty();
        assertThat(priceHistory.findAll()).isEmpty();
        assertThat(transactions.executions()).isZero();
    }

    @Test
    void validateNeverSaves() {
        assertThat(catalog.validate(aValidProduct().build())).isEmpty();
        assertThat(catalog.validate(aValidProduct().name(null).build())).containsOnlyKeys("name");
        assertThat(products.count()).isZero();
    }

    // ---- Edición ---------------------------------------------------------------------

    @Test
    void editingChangesEverythingButTheCodeAndKeepsItsSaleStatus() {
        Product original = catalog.register(aValidProduct().saleEnabled(false).build());

        Product edited = catalog.update(original.id(), aValidProduct().name("Leche sin lactosa")
                .category(ProductCategory.LACTEOS).brand("Campo Verde").netContent("900", ContentUnit.ML)
                .supplierId(dairy.id()).safetyStock(10).buildData());

        assertThat(edited.id()).isEqualTo(original.id());
        assertThat(edited.barcode()).isEqualTo(original.barcode());
        assertThat(edited.name()).isEqualTo("Leche sin lactosa");
        assertThat(edited.brand()).isEqualTo("Campo Verde");
        assertThat(edited.netContent().display()).isEqualTo("900 ml");
        assertThat(edited.supplierId()).isEqualTo(dairy.id());
        assertThat(edited.safetyStock()).isEqualTo(10);
        assertThat(edited.saleEnabled()).isFalse();
        assertThat(products.findById(original.id())).contains(edited);
    }

    @Test
    void aNewPriceOrCostIsAddedToThePriceHistoryButOtherEditsAreNot() {
        Product original = catalog.register(aValidProduct().build());

        catalog.update(original.id(), aValidProduct().name("Otro nombre").buildData());
        catalog.update(original.id(), aValidProduct().salePrice("5500").cost("4100").buildData());

        List<PriceChange> history = priceHistory.findByProduct(original.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).salePrice()).isEqualByComparingTo("5500");
        assertThat(history.get(1).cost()).isEqualByComparingTo("4100");
    }

    @Test
    void anInvalidEditIsRejectedWithAllItsErrors() {
        Product original = catalog.register(aValidProduct().build());

        assertThatThrownBy(() -> catalog.update(original.id(),
                aValidProduct().salePrice("3000").cost("4000").supplierId(retired.id()).buildData()))
                .isInstanceOfSatisfying(InvalidProductException.class,
                        e -> assertThat(e.getFieldErrors()).containsOnlyKeys("salePrice", "supplierId"));
        assertThat(products.findById(original.id())).contains(original);
    }

    @Test
    void validateChangesIgnoresTheBarcodeBecauseItNeverChanges() {
        assertThat(catalog.validateChanges(aValidProduct().barcode("no es un código").buildData())).isEmpty();
        assertThat(catalog.validateChanges(aValidProduct().name(" ").buildData())).containsOnlyKeys("name");
    }

    @Test
    void editingAProductThatDoesNotExistFails() {
        assertThatThrownBy(() -> catalog.update(99L, aValidProduct().buildData()))
                .isInstanceOf(ProductNotFoundException.class);
        assertThatThrownBy(() -> catalog.get(99L)).isInstanceOf(ProductNotFoundException.class);
    }

    // ---- Venta pausada ---------------------------------------------------------------

    @Test
    void pausingTheSaleKeepsTheProductAndItsStockButThePosRefusesIt() {
        Product coffee = catalog.register(aValidProduct().barcode("7703002000049").name("Café molido")
                .perishable(false).initialStock(60, null).build());
        SalesService sales = new SalesService(products, batches, new InMemorySaleRepository(), transactions, clock);

        Product paused = catalog.setSaleEnabled(coffee.id(), false);

        assertThat(paused.saleEnabled()).isFalse();
        assertThat(products.findAllActive()).contains(paused);
        assertThat(batches.all()).extracting(Batch::getQuantityAvailable).containsExactly(60);
        assertThatThrownBy(() -> sales.scan(sales.startSale(), "7703002000049", 1))
                .isInstanceOf(ProductNotForSaleException.class);

        catalog.setSaleEnabled(coffee.id(), true);
        assertThat(sales.scan(sales.startSale(), "7703002000049", 1).unitCount()).isEqualTo(1);
    }

    @Test
    void settingTheSameSaleStatusChangesNothing() {
        Product product = catalog.register(aValidProduct().build());

        assertThat(catalog.setSaleEnabled(product.id(), true)).isEqualTo(product);
    }

    // ---- Catálogo e indicadores ------------------------------------------------------

    @Test
    void onlyActiveSuppliersCanBeChosenSortedByName() {
        suppliers.add("Distribuidora La 14", true);

        assertThat(catalog.activeSuppliers()).extracting(Supplier::name)
                .containsExactly("Distribuidora La 14", "Lácteos del Valle");
    }

    @Test
    void theCatalogShowsSupplierNamesAndMargins() {
        catalog.register(aValidProduct().supplierId(dairy.id()).build());
        catalog.register(aValidProduct().barcode("7703002000018").name("Arroz blanco").salePrice("4800")
                .cost("3600").perishable(false).build());

        List<CatalogEntry> entries = catalog.catalog();

        assertThat(entries).extracting(entry -> entry.product().name())
                .containsExactly("Arroz blanco", "Leche deslactosada");
        assertThat(entries.get(0).supplierName()).isNull();
        assertThat(entries.get(1).supplierName()).isEqualTo("Lácteos del Valle");
        assertThat(entries.get(0).marginPercent()).isEqualTo(25);
    }

    @Test
    void theOverviewCountsProductsOnSalePausedAndWithoutSupplier() {
        catalog.register(aValidProduct().supplierId(dairy.id()).build());
        catalog.register(aValidProduct().barcode("7703002000018").name("Arroz").salePrice("4800").cost("3600")
                .saleEnabled(false).build());

        CatalogOverview overview = catalog.overview();

        assertThat(overview.total()).isEqualTo(2);
        assertThat(overview.onSale()).isEqualTo(1);
        assertThat(overview.paused()).isEqualTo(1);
        assertThat(overview.withoutSupplier()).isEqualTo(1);
        assertThat(overview.averageMarginPercent()).isEqualTo(25);
    }

    @Test
    void anEmptyCatalogHasNoAverageMargin() {
        assertThat(catalog.overview().averageMarginPercent()).isNull();
    }

    @Test
    void marginIsRoundedAndSafeForAZeroPrice() {
        assertThat(entry(3200, 2100).marginPercent()).isEqualTo(34);
        assertThat(entry(5200, 3900).marginPercent()).isEqualTo(25);
        assertThat(entry(0, 0).marginPercent()).isZero();
    }

    private static CatalogEntry entry(int price, int cost) {
        return new CatalogEntry(aProduct().salePrice(price).cost(cost).build(), null);
    }
}
