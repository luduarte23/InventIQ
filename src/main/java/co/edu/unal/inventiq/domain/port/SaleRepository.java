package co.edu.unal.inventiq.domain.port;

import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Sale;

/** C agrega aquí la consulta del historial de ventas que necesita el ROP. */
public interface SaleRepository {

    /** Devuelve la venta con su id asignado. */
    Sale save(Sale sale);

    Optional<Sale> findById(long id);
}
