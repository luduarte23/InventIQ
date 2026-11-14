package com.inventiq.fakes;

import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.SupplierRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso de proveedores: guarda todo en una lista. Solo para pruebas. */
public class InMemorySupplierRepository implements SupplierRepository {

    private final List<Supplier> suppliers = new ArrayList<>();

    /** El puerto no tiene save: las pruebas agregan los proveedores con este método. */
    public Supplier add(Supplier supplier) {
        suppliers.add(supplier);
        return supplier;
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        for (Supplier supplier : suppliers) {
            if (supplier.getId().equals(id)) {
                return Optional.of(supplier);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Supplier> findAllActive() {
        return suppliers.stream().filter(Supplier::isActive).toList();
    }
}
