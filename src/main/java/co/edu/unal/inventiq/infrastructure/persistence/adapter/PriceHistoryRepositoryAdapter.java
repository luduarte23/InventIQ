package co.edu.unal.inventiq.infrastructure.persistence.adapter;

import java.util.List;

import org.springframework.stereotype.Component;

import co.edu.unal.inventiq.domain.model.PriceChange;
import co.edu.unal.inventiq.domain.port.PriceHistoryRepository;
import co.edu.unal.inventiq.infrastructure.persistence.jpa.PriceHistoryJpaRepository;
import co.edu.unal.inventiq.infrastructure.persistence.mapper.PriceHistoryMapper;

@Component
public class PriceHistoryRepositoryAdapter implements PriceHistoryRepository {

    private final PriceHistoryJpaRepository jpa;

    public PriceHistoryRepositoryAdapter(PriceHistoryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void record(PriceChange change) {
        jpa.save(PriceHistoryMapper.toNewEntity(change));
    }

    @Override
    public List<PriceChange> findAll() {
        return jpa.findAllByOrderByProductIdAscChangedAtAscIdAsc().stream().map(PriceHistoryMapper::toDomain).toList();
    }

    @Override
    public List<PriceChange> findByProduct(long productId) {
        return jpa.findByProductIdOrderByChangedAtAscIdAsc(productId).stream().map(PriceHistoryMapper::toDomain).toList();
    }
}
