package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.Sale;
import co.edu.unal.inventiq.domain.model.SoldItem;
import co.edu.unal.inventiq.domain.port.SaleRepository;
import co.edu.unal.inventiq.infrastructure.persistence.entity.SaleEntity;
import co.edu.unal.inventiq.infrastructure.persistence.entity.UserEntity;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.SaleJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.UserJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.SaleMapper;

@Component
public class SaleRepositoryAdapter implements SaleRepository {

    private final SaleJpaRepository jpa;
    private final UserJpaRepository users;

    public SaleRepositoryAdapter(SaleJpaRepository jpa, UserJpaRepository users) {
        this.jpa = jpa;
        this.users = users;
    }

    @Override
    public Sale save(Sale sale) {
        long cashierId = users.findByUsername(sale.cashierUsername()).map(UserEntity::getId)
                .orElseThrow(() -> new IllegalStateException("No existe el usuario " + sale.cashierUsername()));
        SaleEntity saved = jpa.save(SaleMapper.toEntity(sale, cashierId));
        return SaleMapper.toDomain(saved, sale.cashierUsername());
    }

    @Override
    public List<SoldItem> findItemsSoldBetween(LocalDateTime from, LocalDateTime to) {
        return jpa.findItemsSoldBetween(from, to).stream()
                .map(row -> new SoldItem((Long) row[0], (LocalDateTime) row[1], (Long) row[2], (Integer) row[3],
                        (BigDecimal) row[4]))
                .toList();
    }

    @Override
    public Optional<Sale> findById(long id) {
        return jpa.findById(id).map(entity -> SaleMapper.toDomain(entity,
                users.findById(entity.getCashierId()).map(UserEntity::getUsername).orElse("?")));
    }
}
