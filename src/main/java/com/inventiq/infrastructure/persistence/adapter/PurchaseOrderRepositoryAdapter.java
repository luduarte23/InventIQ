package com.inventiq.infrastructure.persistence.adapter;

import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.port.PurchaseOrderRepository;
import com.inventiq.infrastructure.persistence.entity.PurchaseOrderEntity;
import com.inventiq.infrastructure.persistence.jpa.PurchaseOrderJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa el puerto PurchaseOrderRepository del dominio usando JPA (A5). */
@Repository
public class PurchaseOrderRepositoryAdapter implements PurchaseOrderRepository {

    private final PurchaseOrderJpaRepository jpa;

    public PurchaseOrderRepositoryAdapter(PurchaseOrderJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<PurchaseOrder> findById(Long id) {
        return jpa.findById(id).map(PurchaseOrderEntity::toDomain);
    }

    @Override
    public List<PurchaseOrder> findByStatus(OrderStatus status) {
        List<PurchaseOrder> orders = new ArrayList<>();
        for (PurchaseOrderEntity entity : jpa.findByStatusOrderByCreatedAtAsc(status.name())) {
            orders.add(entity.toDomain());
        }
        return orders;
    }

    /** Guarda la orden con sus líneas y devuelve la versión guardada (ya con ids). */
    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        return jpa.save(PurchaseOrderEntity.fromDomain(order)).toDomain();
    }
}
