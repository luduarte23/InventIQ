package co.edu.unal.inventiq.domain.rules;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import co.edu.unal.inventiq.domain.exception.InsufficientStockException;
import co.edu.unal.inventiq.domain.exception.InvalidQuantityException;
import co.edu.unal.inventiq.domain.model.Batch;

/**
 * Regla FIFO (FR-02, tarea B2): reparte una cantidad vendida entre los lotes de un
 * producto. Primero el lote que vence antes; a igual vencimiento, el recibido antes.
 * Los lotes sin vencimiento (no perecederos) van al final. Los vencidos y los vacíos
 * no se venden. Devuelve una asignación por lote tocado.
 */
public class FifoAllocator {

    public static final Comparator<Batch> FIFO_ORDER = Comparator
            .comparing(Batch::getExpirationDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Batch::getReceivedAt)
            .thenComparing(Batch::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * @throws InsufficientStockException si los lotes vendibles no alcanzan; en ese caso
     *                                    no se devuelve ninguna asignación parcial
     */
    public List<BatchAllocation> allocate(String productName, List<Batch> batches, int quantity, LocalDate today) {
        if (quantity <= 0) {
            throw new InvalidQuantityException(quantity);
        }
        List<Batch> sellable = sellableInFifoOrder(batches, today);
        int available = sellable.stream().mapToInt(Batch::getQuantityAvailable).sum();
        if (available < quantity) {
            throw new InsufficientStockException(productName, quantity, available);
        }

        List<BatchAllocation> allocations = new ArrayList<>();
        int pending = quantity;
        for (Batch batch : sellable) {
            if (pending == 0) {
                break;
            }
            int taken = Math.min(pending, batch.getQuantityAvailable());
            allocations.add(new BatchAllocation(batch.getId(), taken));
            pending -= taken;
        }
        return allocations;
    }

    /** Unidades que se pueden vender hoy: suma de los lotes no vencidos. */
    public int sellableQuantity(List<Batch> batches, LocalDate today) {
        return batches.stream()
                .filter(batch -> batch.isSellable(today))
                .mapToInt(Batch::getQuantityAvailable)
                .sum();
    }

    private List<Batch> sellableInFifoOrder(List<Batch> batches, LocalDate today) {
        return batches.stream()
                .filter(batch -> batch.isSellable(today))
                .sorted(FIFO_ORDER)
                .toList();
    }
}
