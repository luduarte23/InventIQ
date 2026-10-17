package co.edu.unal.inventiq.domain.rules;

/** Cuántas unidades se toman de un lote concreto. */
public record BatchAllocation(long batchId, int quantity) {
}
