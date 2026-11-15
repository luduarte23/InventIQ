package com.inventiq.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inventiq.infrastructure.persistence.entity.SupplierEntity;

/** Spring Data genera la consulta a partir del nombre del método. */
public interface SupplierJpaRepository extends JpaRepository<SupplierEntity, Long> {

    List<SupplierEntity> findByActiveTrue();
    List<SupplierEntity> findAllByOrderByNameAsc();
}