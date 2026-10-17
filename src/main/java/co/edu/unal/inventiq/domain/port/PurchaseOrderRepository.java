package co.edu.unal.inventiq.domain.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import co.edu.unal.inventiq.domain.model.OrderStatus;
import co.edu.unal.inventiq.domain.model.PurchaseOrder;

/** Puerto de pedidos de compra con sus líneas. */
public interface PurchaseOrderRepository {

    /** Crea el pedido si no tiene id o guarda sus cambios; lo devuelve con los ids asignados. */
    PurchaseOrder save(PurchaseOrder order);

    Optional<PurchaseOrder> findById(long id);

    /** Pedidos en alguno de esos estados, del más reciente al más antiguo. */
    List<PurchaseOrder> findByStatusIn(Collection<OrderStatus> statuses);

    /**
     * El borrador abierto del proveedor, para no duplicarlo (FR-04).
     *
     * @param supplierId null para buscar el borrador "por revisar"
     */
    Optional<PurchaseOrder> findDraftBySupplier(Long supplierId);
}
