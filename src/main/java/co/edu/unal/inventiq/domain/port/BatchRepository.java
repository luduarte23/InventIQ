package co.edu.unal.inventiq.domain.port;

import java.util.List;

import co.edu.unal.inventiq.domain.exception.ConcurrentStockUpdateException;
import co.edu.unal.inventiq.domain.model.Batch;

public interface BatchRepository {

    /** Lotes del producto con unidades disponibles (incluye vencidos; FifoAllocator los descarta). */
    List<Batch> findAvailableByProduct(long productId);

    /** Todos los lotes con unidades disponibles, de todos los productos. */
    List<Batch> findAllAvailable();

    /** Guarda un lote nuevo (stock inicial; luego también la recepción de pedidos) y lo devuelve con su id. */
    Batch create(Batch batch);

    /**
     * Guarda las cantidades descontadas.
     *
     * @throws ConcurrentStockUpdateException si otro proceso cambió alguno de los lotes
     *                                        desde que se leyó
     */
    void saveAll(List<Batch> batches);
}
