package co.edu.unal.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Product;

/** Puerto del catálogo. Por ahora solo lectura; D4 agrega el registro y la edición. */
public interface ProductRepository {

    Optional<Product> findById(long id);

    Optional<Product> findByBarcode(String barcode);

    /** Productos activos, ordenados por nombre. */
    List<Product> findAllActive();
}
