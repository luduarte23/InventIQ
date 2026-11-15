package com.inventiq.fakes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.SupplierRepository;

/** Repositorio falso de proveedores: guarda todo en una lista. Solo para pruebas. */
public class InMemorySupplierRepository implements SupplierRepository {

    private final List<Supplier> suppliers = new ArrayList<>();

    /** Atajo de las pruebas para dejar proveedores listos. */
    public Supplier add(Supplier supplier) {
        suppliers.add(supplier);
        return supplier;
    }

    /** Si no tiene id le asigna el siguiente; si ya existe lo reemplaza, como la base de datos. */
    @Override
    public Supplier save(Supplier supplier) {
        Supplier saved = supplier;
        if (supplier.getId() == null) {
            long nextId = suppliers.stream().mapToLong(Supplier::getId).max().orElse(0) + 1;
            saved = new Supplier(nextId, supplier.getName(), supplier.getWhatsappNumber(),
                    supplier.getLeadTimeDays(), supplier.getMinOrderAmount(),
                    supplier.getMinOrderUnits(), supplier.isActive());
        }
        suppliers.remove(saved);
        suppliers.add(saved);
        return saved;
    }

    @Override
    public List<Supplier> findAll() {
        return suppliers.stream().sorted(Comparator.comparing(Supplier::getName)).toList();
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
