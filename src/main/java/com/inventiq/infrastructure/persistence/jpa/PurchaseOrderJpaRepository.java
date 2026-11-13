package com.inventiq.infrastructure.persistence.jpa;

import com.inventiq.infrastructure.persistence.entity.PurchaseOrderEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data genera la consulta a partir del nombre del método. */
public interface PurchaseOrderJpaRepository extends JpaRepository<PurchaseOrderEntity, Long> {

    /** Órdenes en un estado, de la más antigua a la más reciente. */
    List<PurchaseOrderEntity> findByStatusOrderByCreatedAtAsc(String status);
}
