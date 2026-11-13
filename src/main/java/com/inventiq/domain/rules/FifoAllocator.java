package com.inventiq.domain.rules;

import com.inventiq.domain.model.Batch;
import com.inventiq.domain.model.exception.InsufficientStockException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Regla FIFO de la venta (FR-02, US-03). Java puro: sin Spring y sin JPA.
 *
 * El código de barras identifica al producto, no al lote. Esta regla decide de qué lotes
 * salen las unidades: primero el que vence antes; a igual fecha, el que se recibió antes;
 * los lotes sin fecha de vencimiento van al final. Los lotes vencidos no se venden.
 */
public class FifoAllocator {

    /**
     * Reparte la cantidad pedida entre los lotes del producto, sin modificarlos.
     *
     * @throws InsufficientStockException si las unidades vendibles no alcanzan
     */
    public List<BatchAllocation> allocate(Long productId, List<Batch> batches, int quantity,
                                          LocalDate today) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity debe ser mayor que 0");
        }

        // 1. Quedarse solo con los lotes vendibles: con unidades y no vencidos.
        List<Batch> sellable = new ArrayList<>();
        int available = 0;
        for (Batch batch : batches) {
            if (batch.getQuantityRemaining() > 0 && !batch.isExpired(today)) {
                sellable.add(batch);
                available += batch.getQuantityRemaining();
            }
        }
        if (available < quantity) {
            throw new InsufficientStockException(productId, quantity, available);
        }

        // 2. Ordenarlos en orden FIFO.
        sellable.sort((a, b) -> compareFifo(a, b));

        // 3. Tomar unidades lote por lote hasta completar la cantidad.
        List<BatchAllocation> allocations = new ArrayList<>();
        int pending = quantity;
        for (Batch batch : sellable) {
            if (pending == 0) {
                break;
            }
            int taken = Math.min(pending, batch.getQuantityRemaining());
            allocations.add(new BatchAllocation(batch, taken));
            pending -= taken;
        }
        return allocations;
    }

    /**
     * Compara dos lotes en orden FIFO. Devuelve un número negativo si 'a' se vende antes
     * que 'b', positivo si se vende después y 0 si da igual.
     */
    public static int compareFifo(Batch a, Batch b) {
        LocalDate expirationA = a.getExpirationDate();
        LocalDate expirationB = b.getExpirationDate();
        if (expirationA != null && expirationB == null) {
            return -1;                                  // el que vence va antes que el que no vence
        }
        if (expirationA == null && expirationB != null) {
            return 1;
        }
        if (expirationA != null && !expirationA.equals(expirationB)) {
            return expirationA.compareTo(expirationB);  // el que vence primero va antes
        }
        return a.getReceivedAt().compareTo(b.getReceivedAt());  // empate: el recibido primero
    }
}
