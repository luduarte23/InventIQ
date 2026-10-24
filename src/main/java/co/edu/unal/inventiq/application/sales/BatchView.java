package co.edu.unal.inventiq.application.sales;

import co.edu.unal.inventiq.domain.model.Batch;
import co.edu.unal.inventiq.domain.model.ExpirationStatus;

/**
 * Un lote tal como se muestra en el inventario.
 *
 * @param daysUntilExpiration negativo si ya venció; null si el lote no vence
 */
public record BatchView(Batch batch, ExpirationStatus status, Long daysUntilExpiration) {
}
