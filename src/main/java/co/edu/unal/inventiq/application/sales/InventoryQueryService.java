package co.edu.unal.inventiq.application.sales;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.port.BatchRepository;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.domain.rules.FifoAllocator;

/**
 * Vista de inventario (B5): stock por producto, sus lotes en orden FIFO con su estado de
 * vencimiento e indicadores generales.
 */
public class InventoryQueryService {

    private final ProductRepository products;
    private final BatchRepository batches;
    private final Clock clock;
    private final FifoAllocator fifo = new FifoAllocator();

    public InventoryQueryService(ProductRepository products, BatchRepository batches, Clock clock) {
        this.products = products;
        this.batches = batches;
        this.clock = clock;
    }

    /** Una sola consulta de lotes para todo el catálogo, no una por producto. */
    public InventoryOverview overview() {
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<Batch>> batchesByProduct = batches.findAllAvailable().stream()
                .collect(Collectors.groupingBy(Batch::getProductId));
        List<ProductStock> rows = products.findAllActive().stream()
                .map(product -> stockOf(product, batchesByProduct.getOrDefault(product.id(), List.of()), today))
                .toList();
        return new InventoryOverview(rows, InventorySummary.of(rows));
    }

    private ProductStock stockOf(Product product, List<Batch> productBatches, LocalDate today) {
        List<BatchView> views = productBatches.stream()
                .sorted(FifoAllocator.FIFO_ORDER)
                .map(batch -> new BatchView(batch, batch.expirationStatus(today), batch.daysUntilExpiration(today)))
                .toList();
        return new ProductStock(product, views,
                fifo.sellableQuantity(productBatches, today),
                unitsWith(views, ExpirationStatus.EXPIRED),
                unitsWith(views, ExpirationStatus.EXPIRING_SOON));
    }

    private static int unitsWith(List<BatchView> views, ExpirationStatus status) {
        return views.stream()
                .filter(view -> view.status() == status)
                .mapToInt(view -> view.batch().getQuantityAvailable())
                .sum();
    }
}
