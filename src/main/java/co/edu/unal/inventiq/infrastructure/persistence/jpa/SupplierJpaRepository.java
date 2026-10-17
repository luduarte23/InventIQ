package co.edu.unal.inventiq.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unal.inventiq.infrastructure.persistence.entity.SupplierEntity;

public interface SupplierJpaRepository extends JpaRepository<SupplierEntity, Long> {

    List<SupplierEntity> findAllByOrderByNameAsc();
}
