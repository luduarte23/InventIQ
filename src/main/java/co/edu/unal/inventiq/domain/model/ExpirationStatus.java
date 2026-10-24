package co.edu.unal.inventiq.domain.model;

import java.time.LocalDate;

/**
 * Estado de un lote según su fecha de vencimiento. Está declarado de lo más urgente a lo menos
 * urgente. "Por vencer" es un aviso para venderlo o descontarlo pronto (FR-07); "vencido" ya no
 * se vende (FifoAllocator lo descarta).
 */
public enum ExpirationStatus {

    EXPIRED("Vencido"),
    EXPIRING_SOON("Por vencer"),
    OK("Vigente"),
    NO_EXPIRATION("Sin vencimiento");

    /** Un lote está "por vencer" si vence hoy o en los próximos 7 días. */
    public static final int EXPIRING_SOON_DAYS = 7;

    private final String label;

    ExpirationStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** @param expiration null para productos no perecederos */
    public static ExpirationStatus classify(LocalDate expiration, LocalDate today) {
        if (expiration == null) {
            return NO_EXPIRATION;
        }
        if (expiration.isBefore(today)) {
            return EXPIRED;
        }
        if (!expiration.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) {
            return EXPIRING_SOON;
        }
        return OK;
    }
}
