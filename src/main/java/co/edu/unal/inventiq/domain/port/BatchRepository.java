package co.edu.unal.inventiq.domain.port;

import java.util.List;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.model.Batch;

/** D6 agrega la creación de lotes al recibir pedidos. */
public interface BatchRepository {

    /** Lotes del producto con unidades disponibles (incluye vencidos; FifoAllocator los descarta). */
    List<Batch> findAvailableByProduct(long productId);

    /** Todos los lotes con unidades disponibles, de todos los productos. */
    List<Batch> findAllAvailable();

    /**
     * Guarda las cantidades descontadas.
     *
     * @throws ConcurrentStockUpdateException si otro proceso cambió alguno de los lotes desde que se leyó
     */
    void saveAll(List<Batch> batches);
}
