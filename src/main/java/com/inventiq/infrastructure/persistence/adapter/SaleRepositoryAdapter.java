package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.Sale;
import com.inventiq.domain.port.SaleRepository;
import com.inventiq.infrastructure.persistence.entity.SaleEntity;
import com.inventiq.infrastructure.persistence.jpa.SaleJpaRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa el puerto SaleRepository del dominio usando JPA (B3). */
@Repository
public class SaleRepositoryAdapter implements SaleRepository {

    private final SaleJpaRepository jpa;

    public SaleRepositoryAdapter(SaleJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda la venta con sus ítems y devuelve la versión guardada (ya con ids). */
    @Override
    public Sale save(Sale sale) {
        return jpa.save(SaleEntity.fromDomain(sale)).toDomain();
    }

    @Override
    public int unitsSoldSince(Long productId, Instant since) {
        return jpa.sumUnitsSoldSince(productId, since).intValue();
    }

    @Override
    public Optional<Sale> findById(Long id) {
        return jpa.findById(id).map(SaleEntity::toDomain);
    }

    /** Borra la venta; sus ítems se borran solos por el cascade de @OneToMany. */
    @Override
    public void delete(Long id) {
        jpa.deleteById(id);
    }
}
