package co.edu.unal.inventiq.domain.port;

import java.util.List;

import co.edu.unal.inventiq.domain.model.PriceChange;

/** Historial de precios de compra (costo) y de venta de los productos. */
public interface PriceHistoryRepository {

    void record(PriceChange change);

    /** Todos los cambios, por producto y en orden cronológico. */
    List<PriceChange> findAll();

    /** Los cambios de un producto, en orden cronológico. */
    List<PriceChange> findByProduct(long productId);
}
