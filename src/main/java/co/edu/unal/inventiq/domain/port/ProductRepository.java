package co.edu.unal.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.exception.DuplicateBarcodeException;
import co.edu.unal.inventiq.domain.model.Product;

/** Puerto del catálogo. */
public interface ProductRepository {

    Optional<Product> findById(long id);

    /** Incluye productos inactivos: un código nunca se reutiliza. */
    Optional<Product> findByBarcode(String barcode);

    /** Productos activos, ordenados por nombre. */
    List<Product> findAllActive();

    /** Todos los productos, activos e inactivos, ordenados por nombre (para el historial). */
    List<Product> findAll();

    /**
     * Guarda un producto nuevo y lo devuelve con su id.
     *
     * @throws DuplicateBarcodeException si otro producto ya tiene ese código
     */
    Product create(Product product);

    /** Guarda los cambios de un producto existente (todo menos el código) y lo devuelve. */
    Product update(Product product);
}
