package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.ProductJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.ProductMapper;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    public ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Product> findById(long id) {
        return jpa.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        return jpa.findByBarcode(barcode).map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAllActive() {
        return jpa.findByActiveTrueOrderByNameAsc().stream().map(ProductMapper::toDomain).toList();
    }
}
