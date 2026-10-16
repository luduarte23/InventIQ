package co.edu.unal.inventiq.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unal.inventiq.infrastructure.persistence.entity.PriceHistoryEntity;

public interface PriceHistoryJpaRepository extends JpaRepository<PriceHistoryEntity, Long> {

    List<PriceHistoryEntity> findAllByOrderByProductIdAscChangedAtAscIdAsc();

    List<PriceHistoryEntity> findByProductIdOrderByChangedAtAscIdAsc(Long productId);
}
