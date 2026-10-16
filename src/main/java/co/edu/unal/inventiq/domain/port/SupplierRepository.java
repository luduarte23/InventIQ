package co.edu.unal.inventiq.domain.port;

import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Supplier;

/** Puerto de proveedores. Por ahora solo lectura; D2 y D4 agregan la escritura. */
public interface SupplierRepository {

    /** Todos los proveedores, activos e inactivos, ordenados por nombre. */
    List<Supplier> findAll();

    Optional<Supplier> findById(long id);
}
