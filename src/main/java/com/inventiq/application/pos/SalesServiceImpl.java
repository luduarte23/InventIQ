package com.inventiq.application.pos;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.Product;
import com.inventiq.domain.model.Sale;
import com.inventiq.domain.model.SaleItem;
import com.inventiq.domain.model.exception.InvalidSaleException;
import com.inventiq.domain.model.exception.UnknownBarcodeException;
import com.inventiq.domain.port.BatchRepository;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.domain.port.SaleRepository;
import com.inventiq.domain.rules.BatchAllocation;
import com.inventiq.domain.rules.FifoAllocator;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Capa de negocio de la venta (B4). Orquesta: busca el producto, le pide a la regla FIFO
 * de qué lotes sacar las unidades, descuenta los lotes y guarda la venta.
 *
 * Solo conoce los puertos del dominio (interfaces). No sabe si detrás hay JPA, H2 o una
 * lista en memoria: por eso las pruebas lo arman a mano con repositorios falsos.
 */
@Service
public class SalesServiceImpl implements SalesService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final SaleRepository saleRepository;
    private final Clock clock;
    private final FifoAllocator fifoAllocator = new FifoAllocator();

    // Spring inyecta las dependencias por este constructor (como en el ejemplo de clase).
    public SalesServiceImpl(ProductRepository productRepository, BatchRepository batchRepository,
                            SaleRepository saleRepository, Clock clock) {
        this.productRepository = productRepository;
        this.batchRepository = batchRepository;
        this.saleRepository = saleRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Sale startSale(Long cashierId) {
        Sale sale = Sale.open(cashierId, Instant.now(clock));
        return saleRepository.save(sale);
    }

    @Override
    @Transactional
    public ScanResult scan(Long saleId, String barcode, int quantity) {
        if (quantity <= 0) {
            throw new InvalidSaleException("La cantidad debe ser mayor que cero.");
        }
        Sale sale = findOpenSale(saleId);

        // 1. Buscar el producto por su código. Si no existe o está inactivo, no se toca nada (US-03 esc. 2).
        String code = barcode == null ? "" : barcode.trim();
        Optional<Product> found = productRepository.findByBarcode(code);
        if (found.isEmpty() || !found.get().isActive()) {
            throw new UnknownBarcodeException(code);
        }
        Product product = found.get();

        // 2. La regla FIFO decide de qué lotes salen las unidades (o lanza InsufficientStockException).
        LocalDate today = LocalDate.now(clock);
        List<Batch> batches = batchRepository.findAvailableByProduct(product.getId(), today);
        List<BatchAllocation> allocations = fifoAllocator.allocate(product.getId(), batches, quantity, today);

        // 3. Descontar cada lote y agregar una línea de venta por lote (US-03 esc. 1).
        List<SaleItem> newItems = new ArrayList<>();
        for (BatchAllocation allocation : allocations) {
            Batch batch = allocation.batch();
            batch.consume(allocation.quantity());
            batchRepository.save(batch);

            BigDecimal price = SaleItem.priceAfterDiscount(product.getSalePrice(), batch.getDiscountPercent());
            SaleItem item = new SaleItem(null, sale.getId(), product.getId(), batch.getId(),
                    allocation.quantity(), price, batch.getDiscountPercent());
            sale.addItem(item);
            newItems.add(item);
        }

        Sale saved = saleRepository.save(sale);
        return new ScanResult(product, newItems, saved.getTotal());
    }

    @Override
    @Transactional
    public Sale finishSale(Long saleId) {
        Sale sale = findOpenSale(saleId);
        if (sale.getItems().isEmpty()) {
            throw new InvalidSaleException("No se puede terminar una venta sin productos.");
        }
        sale.close(Instant.now(clock));
        return saleRepository.save(sale);
    }

    @Override
    @Transactional
    public void cancelSale(Long saleId) {
        Sale sale = findOpenSale(saleId);
        // Cada línea recuerda de qué lote salió: se le devuelven sus unidades a ese mismo lote.
        for (SaleItem item : sale.getItems()) {
            Batch batch = batchRepository.findById(item.getBatchId()).orElseThrow();
            batch.restore(item.getQuantity());
            batchRepository.save(batch);
        }
        saleRepository.delete(saleId);
    }

    /** La venta debe existir y seguir abierta; si no, es un error que se le muestra al cajero. */
    private Sale findOpenSale(Long saleId) {
        Optional<Sale> found = saleRepository.findById(saleId);
        if (found.isEmpty()) {
            throw new InvalidSaleException("La venta " + saleId + " no existe.");
        }
        if (found.get().isClosed()) {
            throw new InvalidSaleException("La venta " + saleId + " ya está cerrada.");
        }
        return found.get();
    }
}
