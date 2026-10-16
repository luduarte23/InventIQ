package co.edu.unal.inventiq.fakes;

import static co.edu.unal.inventiq.fixtures.ProductBuilder.aProduct;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import co.edu.unal.inventiq.domain.exception.DuplicateBarcodeException;
import co.edu.unal.inventiq.domain.exception.ProductNotFoundException;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.port.ProductRepository;

public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> products = new LinkedHashMap<>();
    private long nextId = 1;

    /** Registra un producto activo y a la venta, con precio de venta, y le asigna id. */
    public Product add(String barcode, String name, int salePrice) {
        return add(barcode, name, salePrice, true, null);
    }

    public Product add(String barcode, String name, int salePrice, boolean active, Integer reorderPoint) {
        return create(aProduct().id(null).barcode(barcode).name(name).salePrice(salePrice)
                .cost(salePrice * 7 / 10).perishable(false).active(active).reorderPoint(reorderPoint).build());
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(products.get(id));
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        return products.values().stream().filter(p -> p.barcode().equals(barcode)).findFirst();
    }

    @Override
    public List<Product> findAllActive() {
        return products.values().stream()
                .filter(Product::active)
                .sorted(Comparator.comparing(Product::name))
                .toList();
    }

    @Override
    public List<Product> findAll() {
        return products.values().stream().sorted(Comparator.comparing(Product::name)).toList();
    }

    /** Igual que la base: el código es único. */
    @Override
    public Product create(Product product) {
        if (findByBarcode(product.barcode()).isPresent()) {
            throw new DuplicateBarcodeException(product.barcode());
        }
        Product saved = new Product(nextId++, product.barcode(), product.name(), product.category(), product.brand(),
                product.netContent(), product.description(), product.salePrice(), product.cost(),
                product.perishable(), product.supplierId(), product.safetyStock(), product.reorderPoint(),
                product.active(), product.saleEnabled());
        products.put(saved.id(), saved);
        return saved;
    }

    @Override
    public Product update(Product product) {
        if (!products.containsKey(product.id())) {
            throw new ProductNotFoundException(product.id());
        }
        products.put(product.id(), product);
        return product;
    }

    public int count() {
        return products.size();
    }
}
