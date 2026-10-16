package co.edu.unal.inventiq.application.sales;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.exception.EmptySaleException;
import co.edu.unal.inventiq.domain.exception.InsufficientStockException;
import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;
import co.edu.unal.inventiq.domain.exception.ProductNotForSaleException;
import co.edu.unal.inventiq.domain.exception.UnknownBarcodeException;
import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.Cart;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SaleItem;
import co.edu.unal.inventiq.fakes.DirectTransactionRunner;
import co.edu.unal.inventiq.fakes.InMemoryBatchRepository;
import co.edu.unal.inventiq.fakes.InMemoryProductRepository;
import co.edu.unal.inventiq.fakes.InMemorySaleRepository;

/** Servicio de ventas armado a mano con fakes: sin Spring y sin base de datos. */
class SalesServiceTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 5);
    private static final LocalDateTime RECEIVED = LocalDateTime.of(2026, 9, 28, 9, 0);

    private InMemoryProductRepository products;
    private InMemoryBatchRepository batches;
    private InMemorySaleRepository sales;
    private DirectTransactionRunner transactions;
    private SalesService service;

    private Product yogurt;
    private Batch yogurtOct8;
    private Batch yogurtOct30;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductRepository();
        batches = new InMemoryBatchRepository();
        sales = new InMemorySaleRepository();
        transactions = new DirectTransactionRunner();
        Clock clock = Clock.fixed(TODAY.atTime(10, 0).atZone(BOGOTA).toInstant(), BOGOTA);
        service = new SalesService(products, batches, sales, transactions, clock);

        // US-03: producto con 6 unidades, 3 vencen el 08/10/2026 y 3 el 30/10/2026.
        yogurt = products.add("7702001000011", "Yogur de fresa", 3200);
        yogurtOct8 = batches.add(yogurt.id(), 3, LocalDate.of(2026, 10, 8), RECEIVED);
        yogurtOct30 = batches.add(yogurt.id(), 3, LocalDate.of(2026, 10, 30), RECEIVED.plusDays(1));
    }

    @Test
    void us03Scenario1_scanningTwiceDeductsTwoUnitsFromTheBatchThatExpiresFirst() {
        Cart cart = service.startSale();

        service.scan(cart, "7702001000011", 1);
        service.scan(cart, "7702001000011", 1);
        Sale sale = service.close(cart, "cajero");

        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(1);
        assertThat(batches.quantityOf(yogurtOct30.getId())).isEqualTo(3);
        assertThat(sale.items()).containsExactly(new SaleItem(yogurt.id(), yogurtOct8.getId(), 2, yogurt.salePrice()));
        assertThat(sale.total()).isEqualByComparingTo("6400");
        assertThat(sale.cashierUsername()).isEqualTo("cajero");
        assertThat(sale.createdAt()).isEqualTo(TODAY.atTime(10, 0));
        assertThat(sales.all()).hasSize(1);
    }

    @Test
    void us03Scenario2_unknownBarcodeIsRejectedWithoutChangingStockOrRecordingASale() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 1);

        assertThatThrownBy(() -> service.scan(cart, "0000000000000", 1))
                .isInstanceOf(UnknownBarcodeException.class)
                .hasMessageContaining("0000000000000");

        assertThat(cart.unitCount()).isEqualTo(1);
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(3);
        assertThat(sales.all()).isEmpty();

        // El cajero puede seguir: la venta en curso no se perdió.
        service.close(cart, "cajero");
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(2);
    }

    @Test
    void scanningOnlyValidatesAndNeverTouchesTheStock() {
        Cart cart = service.startSale();

        service.scan(cart, "7702001000011", 2);

        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(3);
        assertThat(batches.saveCalls()).isZero();
    }

    @Test
    void barcodeIsTrimmedBecauseScannersMaySendSpaces() {
        Cart cart = service.startSale();

        service.scan(cart, "  7702001000011 ", 1);

        assertThat(cart.quantityOf(yogurt.id())).isEqualTo(1);
    }

    @Test
    void inactiveProductsCannotBeSold() {
        products.add("7700000000001", "Producto retirado", 1000, false, null);

        assertThatThrownBy(() -> service.scan(service.startSale(), "7700000000001", 1))
                .isInstanceOf(UnknownBarcodeException.class);
    }

    @Test
    void scanningMoreThanTheSellableStockIsRejectedAndTheCartKeepsWhatItHad() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 5);

        assertThatThrownBy(() -> service.scan(cart, "7702001000011", 2))
                .isInstanceOfSatisfying(InsufficientStockException.class, e -> {
                    assertThat(e.getRequested()).isEqualTo(7);
                    assertThat(e.getAvailable()).isEqualTo(6);
                });
        assertThat(cart.quantityOf(yogurt.id())).isEqualTo(5);
    }

    @Test
    void aProductWithPausedSaleCannotBeScannedEvenIfItHasStock() {
        Product coffee = products.create(aProduct().id(null).barcode("7703002000049").name("Café molido")
                .saleEnabled(false).build());
        batches.add(coffee.id(), 60, null, RECEIVED);
        Cart cart = service.startSale();

        assertThatThrownBy(() -> service.scan(cart, "7703002000049", 1))
                .isInstanceOf(ProductNotForSaleException.class)
                .hasMessageContaining("Café molido")
                .hasMessageContaining("Sigue en el inventario");
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void ifTheSaleIsPausedWhileTheProductIsInTheCartClosingFailsAndNothingIsRecorded() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 2);
        products.update(yogurt.withSaleEnabled(false));

        assertThatThrownBy(() -> service.close(cart, "cajero")).isInstanceOf(ProductNotForSaleException.class);

        assertThat(sales.all()).isEmpty();
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(3);
        assertThat(cart.unitCount()).isEqualTo(2);
    }

    @Test
    void expiredBatchesDoNotCountAsStockWhenScanning() {
        Product milk = products.add("7702001000028", "Leche", 4500);
        batches.add(milk.id(), 10, TODAY.minusDays(1), RECEIVED);

        assertThatThrownBy(() -> service.scan(service.startSale(), "7702001000028", 1))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> service.scan(service.startSale(), "7702001000011", 0))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void closingASaleThatSpansTwoBatchesCreatesOneItemPerBatch() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 5);

        Sale sale = service.close(cart, "cajero");

        assertThat(sale.items()).containsExactly(
                new SaleItem(yogurt.id(), yogurtOct8.getId(), 3, yogurt.salePrice()),
                new SaleItem(yogurt.id(), yogurtOct30.getId(), 2, yogurt.salePrice()));
        assertThat(batches.quantityOf(yogurtOct8.getId())).isZero();
        assertThat(batches.quantityOf(yogurtOct30.getId())).isEqualTo(1);
    }

    @Test
    void closingASaleWithSeveralProductsDeductsEachOne() {
        Product rice = products.add("7703002000017", "Arroz", 4800);
        Batch riceBatch = batches.add(rice.id(), 10, null, RECEIVED);
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 1);
        service.scan(cart, "7703002000017", 4);

        Sale sale = service.close(cart, "cajero");

        assertThat(sale.unitCount()).isEqualTo(5);
        assertThat(sale.total()).isEqualByComparingTo("22400");
        assertThat(batches.quantityOf(riceBatch.getId())).isEqualTo(6);
    }

    @Test
    void closingAnEmptySaleIsRejected() {
        assertThatThrownBy(() -> service.close(service.startSale(), "cajero"))
                .isInstanceOf(EmptySaleException.class);
        assertThat(transactions.executions()).isZero();
    }

    @Test
    void ifAnotherCashierSoldTheUnitsMeanwhileClosingFailsAndNothingIsRecorded() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 6);
        batches.sellElsewhere(yogurtOct30.getId(), 2);

        assertThatThrownBy(() -> service.close(cart, "cajero")).isInstanceOf(InsufficientStockException.class);

        assertThat(sales.all()).isEmpty();
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(3);
        assertThat(cart.unitCount()).isEqualTo(6);
    }

    @Test
    void aConcurrentUpdateIsRetriedWithFreshStock() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 2);
        batches.failNextSaves(1);

        Sale sale = service.close(cart, "cajero");

        assertThat(transactions.executions()).isEqualTo(2);
        assertThat(sale.unitCount()).isEqualTo(2);
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(1);
    }

    @Test
    void afterTheMaximumNumberOfConflictsTheCashierIsAskedToRetry() {
        Cart cart = service.startSale();
        service.scan(cart, "7702001000011", 2);
        batches.failNextSaves(SalesService.MAX_CLOSE_ATTEMPTS);

        assertThatThrownBy(() -> service.close(cart, "cajero"))
                .isInstanceOf(ConcurrentStockUpdateException.class);

        assertThat(transactions.executions()).isEqualTo(SalesService.MAX_CLOSE_ATTEMPTS);
        assertThat(sales.all()).isEmpty();
        assertThat(batches.quantityOf(yogurtOct8.getId())).isEqualTo(3);
    }
}
