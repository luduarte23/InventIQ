package com.inventiq.domain.rules;

import com.inventiq.domain.model.Batch;

/**
 * Resultado de la regla FIFO: de qué lote salen cuántas unidades.
 * Una venta que toca dos lotes produce dos BatchAllocation.
 */
public record BatchAllocation(Batch batch, int quantity) {
}
