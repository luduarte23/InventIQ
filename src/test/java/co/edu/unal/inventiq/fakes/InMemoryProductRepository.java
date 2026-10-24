package co.edu.unal.inventiq.fakes;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.port.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> stored = new LinkedHashMap<>();
    private long nextId = 1;

    /** Producto activo, no perecedero y sin punto de reorden calculado. */
    public Product add(String barcode, String name, int salePrice) {
        return add(barcode, name, salePrice, true, null);
    }

    public Product add(String barcode, String name, int salePrice, boolean active, Integer reorderPoint) {
        Product product = new Product(nextId++, barcode, name, BigDecimal.valueOf(salePrice), BigDecimal.ONE,
                false, null, 0, reorderPoint, active);
        stored.put(product.id(), product);
        return product;
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(stored.get(id));
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        return stored.values().stream().filter(p -> p.barcode().equals(barcode)).findFirst();
    }

    @Override
    public List<Product> findAllActive() {
        return stored.values().stream()
                .filter(Product::active)
                .sorted(Comparator.comparing(Product::name))
                .toList();
    }
}
