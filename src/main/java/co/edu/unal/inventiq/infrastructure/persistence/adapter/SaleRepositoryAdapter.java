package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.port.SaleRepository;
import co.edu.unal.inventiq.infrastructure.persistence.entity.SaleEntity;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.SaleJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.SaleMapper;

@Component
public class SaleRepositoryAdapter implements SaleRepository {

    private final SaleJpaRepository jpa;

    public SaleRepositoryAdapter(SaleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Sale save(Sale sale) {
        long cashierId = jpa.findUserIdByUsername(sale.cashierUsername())
                .orElseThrow(() -> new IllegalStateException("No existe el usuario " + sale.cashierUsername()));
        SaleEntity saved = jpa.save(SaleMapper.toEntity(sale, cashierId));
        return SaleMapper.toDomain(saved, sale.cashierUsername());
    }

    @Override
    public Optional<Sale> findById(long id) {
        return jpa.findById(id).map(entity -> SaleMapper.toDomain(entity,
                jpa.findUsernameByUserId(entity.getCashierId()).orElse("?")));
    }
}
