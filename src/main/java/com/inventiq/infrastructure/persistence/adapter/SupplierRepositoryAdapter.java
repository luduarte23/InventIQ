package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Supplier;
import com.inventiq.domain.port.SupplierRepository;
import com.inventiq.infrastructure.persistence.entity.SupplierEntity;
import com.inventiq.infrastructure.persistence.jpa.SupplierJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa el puerto SupplierRepository del dominio usando JPA (A5). */
@Repository
public class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    public SupplierRepositoryAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Supplier> findById(Long id) {
        return jpa.findById(id).map(SupplierEntity::toDomain);
    }

    @Override
    public List<Supplier> findAllActive() {
        List<Supplier> suppliers = new ArrayList<>();
        for (SupplierEntity entity : jpa.findByActiveTrue()) {
            suppliers.add(entity.toDomain());
        }
        return suppliers;
    }
}
