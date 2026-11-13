package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.ProductEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data genera la implementación de esta interfaz. JpaRepository ya trae save,
 * findById, deleteById...; los demás métodos se crean a partir de su nombre.
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByBarcode(String barcode);

    List<ProductEntity> findByActiveTrue();
}
