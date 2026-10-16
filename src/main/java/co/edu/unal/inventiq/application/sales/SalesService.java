package co.edu.unal.inventiq.application.sales;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.edu.unal.inventiq.application.TransactionRunner;
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
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.port.SaleRepository;
import co.edu.unal.inventiq.domain.rules.BatchAllocation;
import co.edu.unal.inventiq.domain.rules.FifoAllocator;

/**
 * Venta por código de barras con deducción FIFO (FR-02, US-03; tareas B4 y B7).
 *
 * <ul>
 *   <li>{@link #startSale()}: carrito vacío.</li>
 *   <li>{@link #scan}: valida el código y el stock y agrega al carrito. No toca la base.</li>
 *   <li>{@link #close}: en una transacción, reparte cada línea entre lotes (FIFO), descuenta
 *       y registra la venta. Si otro proceso cambió los lotes, reintenta.</li>
 * </ul>
 */
public class SalesService {

    static final int MAX_CLOSE_ATTEMPTS = 3;

    private final ProductRepository products;
    private final BatchRepository batches;
    private final SaleRepository sales;
    private final TransactionRunner transactions;
    private final Clock clock;
    private final FifoAllocator fifo = new FifoAllocator();

    public SalesService(ProductRepository products, BatchRepository batches, SaleRepository sales,
                        TransactionRunner transactions, Clock clock) {
        this.products = products;
        this.batches = batches;
        this.sales = sales;
        this.transactions = transactions;
        this.clock = clock;
    }

    public Cart startSale() {
        return new Cart();
    }

    /**
     * @throws UnknownBarcodeException     si el código no es de un producto activo (US-03 esc. 2)
     * @throws ProductNotForSaleException  si el producto tiene la venta pausada
     * @throws InsufficientStockException  si lo acumulado en el carrito supera el stock vendible
     * @throws InvalidQuantityException    si la cantidad no es positiva
     */
    public Cart scan(Cart cart, String barcode, int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        String code = barcode == null ? "" : barcode.strip();
        Product product = products.findByBarcode(code)
                .filter(Product::active)
                .orElseThrow(() -> new UnknownBarcodeException(code));
        if (!product.saleEnabled()) {
            throw new ProductNotForSaleException(product.name());
        }

        int requested = cart.quantityOf(product.id()) + quantity;
        int sellable = fifo.sellableQuantity(batches.findAvailableByProduct(product.id()), today());
        if (requested > sellable) {
            throw new InsufficientStockException(product.name(), requested, sellable);
        }
        cart.add(product, quantity);
        return cart;
    }

    /**
     * Cierra la venta. Si falla, el carrito queda como estaba para que el cajero corrija y reintente.
     *
     * @throws EmptySaleException             si el carrito está vacío
     * @throws InsufficientStockException     si mientras tanto otro cajero vendió esas unidades
     * @throws ConcurrentStockUpdateException si tras {@value #MAX_CLOSE_ATTEMPTS} intentos sigue
     *                                        habiendo cambios concurrentes
     */
    public Sale close(Cart cart, String cashierUsername) {
        if (cart.isEmpty()) {
            throw new EmptySaleException();
        }
        ConcurrentStockUpdateException lastConflict = null;
        for (int attempt = 1; attempt <= MAX_CLOSE_ATTEMPTS; attempt++) {
            try {
                return transactions.inTransaction(() -> registerSale(cart, cashierUsername));
            } catch (ConcurrentStockUpdateException conflict) {
                lastConflict = conflict;
            }
        }
        throw lastConflict;
    }

    private Sale registerSale(Cart cart, String cashierUsername) {
        LocalDate today = today();
        List<SaleItem> items = new ArrayList<>();
        List<Batch> touched = new ArrayList<>();

        for (Cart.CartLine line : cart.lines()) {
            // La venta pudo pausarse mientras el producto estaba en el carrito.
            products.findById(line.productId())
                    .filter(Product::isSellable)
                    .orElseThrow(() -> new ProductNotForSaleException(line.productName()));
            Map<Long, Batch> productBatches = batches.findAvailableByProduct(line.productId()).stream()
                    .collect(Collectors.toMap(Batch::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
            List<BatchAllocation> allocations =
                    fifo.allocate(line.productName(), List.copyOf(productBatches.values()), line.quantity(), today);

            for (BatchAllocation allocation : allocations) {
                Batch batch = productBatches.get(allocation.batchId());
                batch.deduct(allocation.quantity());
                touched.add(batch);
                items.add(new SaleItem(line.productId(), allocation.batchId(), allocation.quantity(), line.unitPrice()));
            }
        }

        batches.saveAll(touched);
        return sales.save(new Sale(null, cashierUsername, LocalDateTime.now(clock), items));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
