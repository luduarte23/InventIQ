package com.inventiq.fakes;

import com.inventiq.domain.model.OrderStatus;
import com.inventiq.domain.model.PurchaseOrder;
import com.inventiq.domain.port.PurchaseOrderRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Repositorio falso de pedidos: guarda todo en una lista. Solo para pruebas. */
public class InMemoryPurchaseOrderRepository implements PurchaseOrderRepository {

    private final List<PurchaseOrder> orders = new ArrayList<>();
    private long nextId = 1;

    /** Si el pedido es nuevo le pone un id; si ya existe, lo reemplaza. */
    @Override
    public PurchaseOrder save(PurchaseOrder order) {
        PurchaseOrder saved = order;
        if (order.getId() == null) {
            saved = new PurchaseOrder(nextId++, order.getSupplierId(), order.getStatus(),
                    order.isNeedsReview(), order.isDeferredByBudget(), order.getCreatedAt(),
                    order.getApprovedAt(), order.getSentAt(), order.getReceivedAt(),
                    order.getLines());
        }
        orders.remove(saved);
        orders.add(saved);
        return saved;
    }

    @Override
    public Optional<PurchaseOrder> findById(Long id) {
        for (PurchaseOrder order : orders) {
            if (order.getId().equals(id)) {
                return Optional.of(order);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<PurchaseOrder> findByStatus(OrderStatus status) {
        return orders.stream().filter(o -> o.getStatus() == status).toList();
    }
}
