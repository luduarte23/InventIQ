package co.edu.unal.inventiq.infrastructure.persistence;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import co.edu.unal.inventiq.domain.exception.DuplicateBarcodeException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.ContentUnit;
import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.ProductCategory;
import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.BatchRepositoryAdapter;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.PriceHistoryRepositoryAdapter;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.ProductRepositoryAdapter;
import co.edu.unal.inventiq.infrastructure.persistence.adapter.SupplierRepositoryAdapter;

/** Catálogo, proveedores e historial de precios contra H2 con el esquema de Flyway. */
@DataJpaTest
@Import({ProductRepositoryAdapter.class, BatchRepositoryAdapter.class, SupplierRepositoryAdapter.class,
        PriceHistoryRepositoryAdapter.class})
class CatalogPersistenceTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ProductRepositoryAdapter products;

    @Autowired
    private BatchRepositoryAdapter batches;

    @Autowired
    private SupplierRepositoryAdapter suppliers;

    @Autowired
    private PriceHistoryRepositoryAdapter priceHistory;

    @Test
    void aCreatedProductKeepsAllItsDetails() {
        Product created = products.create(product("7702001000029"));

        assertThat(created.id()).isNotNull();
        assertThat(products.findByBarcode("7702001000029")).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(created.id());
            assertThat(found.name()).isEqualTo("Leche deslactosada");
            assertThat(found.category()).isEqualTo(ProductCategory.LACTEOS);
            assertThat(found.brand()).isEqualTo("La Pradera");
            assertThat(found.netContent().display()).isEqualTo("1,5 L");
            assertThat(found.description()).isEqualTo("En bolsa.");
            assertThat(found.salePrice()).isEqualByComparingTo("5200");
            assertThat(found.active()).isTrue();
            assertThat(found.saleEnabled()).isTrue();
        });
    }

    @Test
    void productsWithoutContentOrBrandAreAllowed() {
        Product created = products.create(aProduct().id(null).barcode("7704003000014").name("Pan").brand(null)
                .netContent(null, null).build());

        assertThat(products.findById(created.id())).hasValueSatisfying(found -> {
            assertThat(found.brand()).isNull();
            assertThat(found.netContent()).isNull();
        });
    }

    @Test
    void theDatabaseRejectsADuplicateCode() {
        products.create(product("7702001000029"));

        assertThatThrownBy(() -> products.create(product("7702001000029")))
                .isInstanceOf(DuplicateBarcodeException.class);
    }

    @Test
    void anUpdateSavesTheChangesAndThePausedSale() {
        Product created = products.create(product("7702001000029"));

        Product updated = products.update(new Product(created.id(), created.barcode(), "Leche sin lactosa",
                ProductCategory.LACTEOS, "Campo Verde", created.netContent(), null, new BigDecimal("5600"),
                new BigDecimal("4000"), true, null, 8, null, true, false));

        assertThat(updated.name()).isEqualTo("Leche sin lactosa");
        assertThat(products.findById(created.id())).hasValueSatisfying(found -> {
            assertThat(found.brand()).isEqualTo("Campo Verde");
            assertThat(found.salePrice()).isEqualByComparingTo("5600");
            assertThat(found.safetyStock()).isEqualTo(8);
            assertThat(found.saleEnabled()).isFalse();
        });
        assertThat(products.findAll()).extracting(Product::name).containsExactly("Leche sin lactosa");
    }

    @Test
    void aCreatedBatchGetsAnIdAndIsAvailableForSale() {
        Product product = products.create(product("7702001000029"));
        LocalDate expiration = LocalDate.of(2026, 11, 1);

        Batch created = batches.create(new Batch(null, product.id(), 12, 12, expiration,
                LocalDateTime.of(2026, 10, 16, 10, 0), 0));

        assertThat(created.getId()).isNotNull();
        assertThat(batches.findAvailableByProduct(product.id())).singleElement().satisfies(batch -> {
            assertThat(batch.getId()).isEqualTo(created.getId());
            assertThat(batch.getQuantityAvailable()).isEqualTo(12);
            assertThat(batch.getExpirationDate()).isEqualTo(expiration);
        });
    }

    @Test
    void thePriceHistoryIsReturnedByProductInChronologicalOrder() {
        Product milk = products.create(product("7702001000029"));
        Product rice = products.create(aProduct().id(null).barcode("7703002000018").name("Arroz").build());
        priceHistory.record(change(milk.id(), 5400, 4000, LocalDateTime.of(2026, 10, 1, 9, 0)));
        priceHistory.record(change(milk.id(), 5000, 3700, LocalDateTime.of(2026, 6, 1, 9, 0)));
        priceHistory.record(change(rice.id(), 4800, 3600, LocalDateTime.of(2026, 8, 1, 9, 0)));

        assertThat(priceHistory.findByProduct(milk.id())).extracting(PriceChange::salePrice)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("5000"), new BigDecimal("5400"));
        assertThat(priceHistory.findAll()).extracting(PriceChange::productId)
                .containsExactly(milk.id(), milk.id(), rice.id());
    }

    @Test
    void suppliersAreListedByNameAndFoundById() {
        jdbc.update("insert into supplier (name, lead_time_days, active) values ('Zeta Distribuciones', 5, true)");
        jdbc.update("insert into supplier (name, lead_time_days, active) values ('Alfa Lácteos', 2, false)");
        long alfaId = jdbc.queryForObject("select id from supplier where name = 'Alfa Lácteos'", Long.class);

        assertThat(suppliers.findAll()).extracting(Supplier::name)
                .containsExactly("Alfa Lácteos", "Zeta Distribuciones");
        assertThat(suppliers.findById(alfaId)).hasValueSatisfying(alfa -> {
            assertThat(alfa.leadTimeDays()).isEqualTo(2);
            assertThat(alfa.active()).isFalse();
        });
        assertThat(suppliers.findById(999_999)).isEmpty();
    }

    private static Product product(String barcode) {
        return aProduct().id(null).barcode(barcode).name("Leche deslactosada").netContent("1.5", ContentUnit.L)
                .description("En bolsa.").salePrice(5200).cost(3900).build();
    }

    private static PriceChange change(long productId, int price, int cost, LocalDateTime at) {
        return new PriceChange(null, productId, BigDecimal.valueOf(price), BigDecimal.valueOf(cost), at);
    }
}
