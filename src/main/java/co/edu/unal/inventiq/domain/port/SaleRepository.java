package co.edu.unal.inventiq.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SoldItem;

public interface SaleRepository {

    /** Devuelve la venta con su id asignado. */
    Sale save(Sale sale);

    Optional<Sale> findById(long id);

    /**
     * Las líneas vendidas en [from, to), en orden cronológico. Lo usan el historial y, más
     * adelante, la velocidad de ventas del ROP (C).
     */
    List<SoldItem> findItemsSoldBetween(LocalDateTime from, LocalDateTime to);
}
