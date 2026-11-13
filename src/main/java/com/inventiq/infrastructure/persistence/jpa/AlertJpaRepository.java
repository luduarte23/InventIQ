package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.AlertEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la consulta a partir del nombre del método. */
public interface AlertJpaRepository extends JpaRepository<AlertEntity, Long> {

    /** Alertas sin resolver, de la más reciente a la más antigua. */
    List<AlertEntity> findByResolvedFalseOrderByCreatedAtDesc();
}