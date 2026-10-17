package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.exception.DuplicateBarcodeException;
import co.edu.unal.inventiq.domain.exception.ProductNotFoundException;
import co.edu.unal.inventiq.domain.model.Product;
import co.edu.unal.inventiq.domain.port.ProductRepository;
import co.edu.unal.inventiq.infrastructure.persistence.entity.ProductEntity;
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

    @Override
    public List<Product> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(ProductMapper::toDomain).toList();
    }

    /**
     * CatalogService ya revisó que el código esté libre; si aun así la base lo rechaza es
     * porque otro registro simultáneo usó el mismo código (la única restricción única de
     * product es barcode, y el proveedor ya se validó).
     */
    @Override
    public Product create(Product product) {
        try {
            return ProductMapper.toDomain(jpa.saveAndFlush(ProductMapper.toNewEntity(product)));
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateBarcodeException(product.barcode());
        }
    }

    @Override
    public Product update(Product product) {
        ProductEntity entity = jpa.findById(product.id())
                .orElseThrow(() -> new ProductNotFoundException(product.id()));
        ProductMapper.copyInto(product, entity);
        return ProductMapper.toDomain(jpa.saveAndFlush(entity));
    }
}
