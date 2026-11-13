package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Product;
import com.inventiq.domain.port.ProductRepository;
import com.inventiq.infrastructure.persistence.entity.ProductEntity;
import com.inventiq.infrastructure.persistence.jpa.ProductJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * Implementa el puerto ProductRepository del dominio usando JPA. Traduce entre
 * Product (dominio) y ProductEntity (tabla), así JPA no se filtra a la capa de negocio.
 */
@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    public ProductRepositoryAdapter(ProductJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpa.findById(id).map(ProductEntity::toDomain);
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        return jpa.findByBarcode(barcode).map(ProductEntity::toDomain);
    }

    @Override
    public List<Product> findAllActive() {
        List<Product> products = new ArrayList<>();
        for (ProductEntity entity : jpa.findByActiveTrue()) {
            products.add(entity.toDomain());
        }
        return products;
    }

    @Override
    public Product save(Product product) {
        return jpa.save(ProductEntity.fromDomain(product)).toDomain();
    }
}
