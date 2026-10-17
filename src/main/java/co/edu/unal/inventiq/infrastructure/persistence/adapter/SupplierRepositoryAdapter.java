package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.Supplier;
import co.edu.unal.inventiq.domain.port.SupplierRepository;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.SupplierJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.SupplierMapper;

@Component
public class SupplierRepositoryAdapter implements SupplierRepository {

    private final SupplierJpaRepository jpa;

    public SupplierRepositoryAdapter(SupplierJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<Supplier> findAll() {
        return jpa.findAllByOrderByNameAsc().stream().map(SupplierMapper::toDomain).toList();
    }

    @Override
    public Optional<Supplier> findById(long id) {
        return jpa.findById(id).map(SupplierMapper::toDomain);
    }
}
