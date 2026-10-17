package co.edu.unal.inventiq.fakes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.domain.port.SupplierRepository;

public class InMemorySupplierRepository implements SupplierRepository {

    private final Map<Long, Supplier> suppliers = new LinkedHashMap<>();
    private long nextId = 1;

    public Supplier add(String name, boolean active) {
        Supplier supplier = new Supplier(nextId++, name, "+573000000000", 3, null, active);
        suppliers.put(supplier.id(), supplier);
        return supplier;
    }

    @Override
    public List<Supplier> findAll() {
        return suppliers.values().stream().sorted(Comparator.comparing(Supplier::name)).toList();
    }

    @Override
    public Optional<Supplier> findById(long id) {
        return Optional.ofNullable(suppliers.get(id));
    }
}
