package co.edu.unal.inventiq.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unal.inventiq.infrastructure.persistence.entity.ProductEntity;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByBarcode(String barcode);

    List<ProductEntity> findByActiveTrueOrderByNameAsc();

    List<ProductEntity> findAllByOrderByNameAsc();
}
