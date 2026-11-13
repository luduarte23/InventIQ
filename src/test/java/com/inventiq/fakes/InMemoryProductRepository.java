package com.inventiq.fakes;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.port.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso: guarda los productos en una lista, sin base de datos. Solo para pruebas. */
public class InMemoryProductRepository implements ProductRepository {

    private final List<Product> products = new ArrayList<>();

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        for (Product product : products) {
            if (product.getBarcode().equals(barcode)) {
                return Optional.of(product);
            }
        }
        return Optional.empty();
    }

    @Override
    public Product save(Product product) {
        products.add(product);
        return product;
    }

    // La venta no usa estos dos métodos.
    @Override
    public Optional<Product> findById(Long id) {
        return Optional.empty();
    }

    @Override
    public List<Product> findAllActive() {
        return products;
    }
}
