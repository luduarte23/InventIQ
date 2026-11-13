package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.BatchEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la consulta SQL a partir del nombre de cada método. */
public interface BatchJpaRepository extends JpaRepository<BatchEntity, Long> {

    /** Lotes de un producto con más de 'quantity' unidades restantes. */
    List<BatchEntity> findByProductIdAndQuantityRemainingGreaterThan(Long productId, int quantity);

    /** Lotes con unidades que vencen antes de 'limit'. */
    List<BatchEntity> findByExpirationDateBeforeAndQuantityRemainingGreaterThan(LocalDate limit, int quantity);
}
